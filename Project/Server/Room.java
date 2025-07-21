// Updated: 2025-07-20 | UCID: Mi348
package Project.Server;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Payload;
import Project.Common.RoomAction;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;
import Project.Exceptions.DuplicateRoomException;
import Project.Exceptions.RoomNotFoundException;

public class Room implements AutoCloseable {
    private final String name;
    private volatile boolean isRunning = false;
    private final ConcurrentHashMap<Long, ServerThread> clientsInRoom = new ConcurrentHashMap<>();
    public final static String LOBBY = "lobby";
    private GameSession gameSession;

    private void info(String message) {
        LoggerUtil.INSTANCE.info(TextFX.colorize(String.format("Room[%s]: %s", name, message), Color.PURPLE));
    }

    public Room(String name) {
        this.name = name;
        this.isRunning = true;
        this.gameSession = new GameSession(this); // initialize game session
        info("Created");
    }

    public String getName() {
        return this.name;
    }

    public Collection<ServerThread> getClients() {
        return clientsInRoom.values();
    }

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning || clientsInRoom.containsKey(client.getClientId())) return;
        clientsInRoom.put(client.getClientId(), client);
        client.setCurrentRoom(this);
        client.sendResetUserList();
        syncExistingClients(client);
        joinStatusRelay(client, true);
    }

    protected synchronized void removeClient(ServerThread client) {
        if (!isRunning || !clientsInRoom.containsKey(client.getClientId())) return;
        ServerThread removedClient = clientsInRoom.remove(client.getClientId());
        if (removedClient != null) {
            joinStatusRelay(removedClient, false);
            autoCleanup();
        }
    }

    private void syncExistingClients(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverThread -> {
            if (serverThread.getClientId() != incomingClient.getClientId()) {
                boolean failed = !incomingClient.sendClientInfo(serverThread.getClientId(), serverThread.getClientName(), RoomAction.JOIN, true);
                if (failed) disconnect(serverThread);
            }
        });
    }

    private void joinStatusRelay(ServerThread client, boolean didJoin) {
        clientsInRoom.values().removeIf(serverThread -> {
            boolean failedToSync = !serverThread.sendClientInfo(client.getClientId(), client.getClientName(), didJoin ? RoomAction.JOIN : RoomAction.LEAVE);
            boolean failedToSend = !serverThread.sendMessage(client.getClientId(), String.format("Room[%s] %s %s the room", getName(), client.getDisplayName(), didJoin ? "joined" : "left"));
            if (failedToSend || failedToSync) {
                disconnect(serverThread);
            }
            return failedToSend;
        });
    }

    protected synchronized void relay(ServerThread sender, String message) {
        String senderString = sender == null ? "Room[" + getName() + "]" : sender.getDisplayName();
        long senderId = sender == null ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        final String formattedMessage = String.format("%s: %s", senderString, message);
        clientsInRoom.values().removeIf(serverThread -> !serverThread.sendMessage(senderId, formattedMessage));
    }

    private synchronized void disconnect(ServerThread client) {
        ServerThread removed = clientsInRoom.remove(client.getClientId());
        if (removed != null) {
            clientsInRoom.values().removeIf(serverThread -> !serverThread.sendClientInfo(removed.getClientId(), removed.getClientName(), RoomAction.LEAVE));
            relay(null, removed.getDisplayName() + " disconnected");
            removed.disconnect();
        }
        autoCleanup();
    }

    protected synchronized void disconnectAll() {
        clientsInRoom.values().forEach(this::disconnect);
    }

    private void autoCleanup() {
        if (!LOBBY.equalsIgnoreCase(name) && clientsInRoom.isEmpty()) {
            close();
        }
    }

    @Override
    public void close() {
        relay(null, "Room is shutting down. Moving everyone to lobby.");
        clientsInRoom.values().forEach(client -> {
            try {
                Server.INSTANCE.joinRoom(LOBBY, client);
            } catch (RoomNotFoundException e) {
                LoggerUtil.INSTANCE.severe("Lobby not found", e);
            }
        });
        clientsInRoom.clear();
        Server.INSTANCE.removeRoom(this);
        isRunning = false;
        info("Room closed");
    }

    // Milestone 2: broadcast payload to room
    public synchronized void broadcastPayload(Payload payload) {
        clientsInRoom.values().removeIf(client -> !client.sendToClient(payload));
    }

    // Room command handlers
    public void handleListRooms(ServerThread sender, String roomQuery) {
        sender.sendRooms(Server.INSTANCE.listRooms(roomQuery));
    }

    public void handleCreateRoom(ServerThread sender, String roomName) {
        try {
            Server.INSTANCE.createRoom(roomName);
            Server.INSTANCE.joinRoom(roomName, sender);
        } catch (DuplicateRoomException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Room already exists");
        } catch (RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Error joining newly created room");
        }
    }

    public void handleJoinRoom(ServerThread sender, String roomName) {
        try {
            Server.INSTANCE.joinRoom(roomName, sender);
        } catch (RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Room does not exist");
        }
    }

    // ✅ Made public to fix red underline in BaseServerThread.java
    public synchronized void handleDisconnect(ServerThread sender) {
        disconnect(sender);
    }

    protected synchronized void handleReverseText(ServerThread sender, String text) {
        String reversed = new StringBuilder(text).reverse().toString();
        relay(sender, reversed);
    }

    protected synchronized void handleMessage(ServerThread sender, String text) {
        relay(sender, text);
    }

    // Milestone 2: handle GAME_READY
    protected synchronized void handleGameReady(ServerThread sender) {
        gameSession.markReady(sender, clientsInRoom.values());
    }

    // Milestone 2: handle GAME_PICK
    protected synchronized void handlePlayerPick(ServerThread sender, String pick) {
        gameSession.registerPick(sender, pick);
    }
}
