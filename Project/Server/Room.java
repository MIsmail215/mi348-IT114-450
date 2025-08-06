package Project.Server;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import Project.Common.*;
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
        this.gameSession = new GameSession(this);
        info("Created");
    }
    
    public synchronized void handleGameReady(ServerThread sender, Payload payload) {
        gameSession.markReady(sender, payload);
    }
    
    public void handleToggleAway(ServerThread sender) {
        gameSession.toggleAwayStatus(sender);
    }
    
    protected synchronized void addSpectator(ServerThread client) {
        if (!isRunning || clientsInRoom.containsKey(client.getClientId())) return;
        client.setSpectator(true);
        clientsInRoom.put(client.getClientId(), client);
        client.setCurrentRoom(this);
        client.sendResetUserList();
        syncExistingClients(client);
        joinStatusRelay(client, true);
        relay(null, client.getDisplayName() + " is now spectating.");
    }

    private void syncExistingClients(ServerThread incomingClient) {
        clientsInRoom.values().forEach(existingClient -> {
            if (existingClient.getClientId() != incomingClient.getClientId()) {
                incomingClient.sendClientInfo(existingClient.getClientId(), existingClient.getClientName(), RoomAction.JOIN, true, existingClient.isSpectator());
            }
        });
    }

    private void joinStatusRelay(ServerThread client, boolean didJoin) {
        clientsInRoom.values().forEach(serverThread -> {
            serverThread.sendClientInfo(serverThread.getClientId(), client.getClientName(), didJoin ? RoomAction.JOIN : RoomAction.LEAVE, false, client.isSpectator());
        });
        if (didJoin && !client.isSpectator()) {
            relay(null, String.format("%s joined the room", client.getDisplayName()));
        } else if (!didJoin) {
            relay(null, String.format("%s left the room", client.getDisplayName()));
        }
    }

    protected synchronized void relay(ServerThread sender, String message) {
        if (sender != null && sender.isSpectator()) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot send messages.");
            return;
        }
        String senderString = sender == null ? "Room[" + getName() + "]" : sender.getDisplayName();
        long senderId = sender == null ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        final String formattedMessage = String.format("%s: %s", senderString, message);
        clientsInRoom.values().removeIf(serverThread -> !serverThread.sendMessage(senderId, formattedMessage));
    }
    
    public String getName() { return this.name; }
    public Collection<ServerThread> getClients() { return clientsInRoom.values(); }
    protected synchronized void addClient(ServerThread client) {
        if (!isRunning || clientsInRoom.containsKey(client.getClientId())) return;
        client.setSpectator(false);
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
    private synchronized void disconnect(ServerThread client) {
        ServerThread removed = clientsInRoom.remove(client.getClientId());
        if (removed != null) {
            clientsInRoom.values().removeIf(serverThread -> !serverThread.sendClientInfo(removed.getClientId(), removed.getClientName(), RoomAction.LEAVE, false, removed.isSpectator()));
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
    public synchronized void broadcastPayload(Payload payload) {
        clientsInRoom.values().removeIf(client -> !client.sendToClient(payload));
    }
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
    protected synchronized void handlePlayerPick(ServerThread sender, String pick) {
        gameSession.registerPick(sender, pick);
    }
}