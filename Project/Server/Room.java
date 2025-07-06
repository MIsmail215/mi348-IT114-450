//UCID:Mi348
package Project.Server;

import java.util.concurrent.ConcurrentHashMap;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.RoomAction;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;
import Project.Exceptions.DuplicateRoomException;
import Project.Exceptions.RoomNotFoundException;

public class Room implements AutoCloseable {
    private final String name;// unique name of the Room
    private volatile boolean isRunning = false;
    private final ConcurrentHashMap<Long, ServerThread> clientsInRoom = new ConcurrentHashMap<Long, ServerThread>();

    public final static String LOBBY = "lobby";

    private void info(String message) {
        LoggerUtil.INSTANCE.info(TextFX.colorize(String.format("Room[%s]: %s", name, message), Color.PURPLE));
    }

    public Room(String name) {
        this.name = name;
        isRunning = true;
        info("Created");
    }

    public String getName() {
        return this.name;
    }

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning) return;
        if (clientsInRoom.containsKey(client.getClientId())) {
            info("Attempting to add a client that already exists in the room");
            return;
        }
        clientsInRoom.put(client.getClientId(), client);
        client.setCurrentRoom(this);
        client.sendResetUserList();
        syncExistingClients(client);
        joinStatusRelay(client, true);
    }

    protected synchronized void removeClient(ServerThread client) {
        if (!isRunning) return;
        if (!clientsInRoom.containsKey(client.getClientId())) {
            info("Attempting to remove a client that doesn't exist in the room");
            return;
        }
        ServerThread removedClient = clientsInRoom.get(client.getClientId());
        if (removedClient != null) {
            joinStatusRelay(removedClient, false);
            clientsInRoom.remove(client.getClientId());
            autoCleanup();
        }
    }

    private void syncExistingClients(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverThread -> {
            if (serverThread.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendClientInfo(serverThread.getClientId(),
                        serverThread.getClientName(), RoomAction.JOIN, true);
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverThread.getDisplayName()));
                    disconnect(serverThread);
                }
            }
        });
    }

    private void joinStatusRelay(ServerThread client, boolean didJoin) {
        clientsInRoom.values().removeIf(serverThread -> {
            String formattedMessage = String.format("Room[%s] %s %s the room",
                    getName(),
                    client.getClientId() == serverThread.getClientId() ? "You"
                            : client.getDisplayName(),
                    didJoin ? "joined" : "left");
            final long senderId = client == null ? Constants.DEFAULT_CLIENT_ID : client.getClientId();
            boolean failedToSync = !serverThread.sendClientInfo(client.getClientId(),
                    client.getClientName(), didJoin ? RoomAction.JOIN : RoomAction.LEAVE);
            boolean failedToSend = !serverThread.sendMessage(senderId, formattedMessage);
            if (failedToSend || failedToSync) {
                LoggerUtil.INSTANCE.warning(
                        String.format("Removing disconnected %s from list", serverThread.getDisplayName()));
                disconnect(serverThread);
            }
            return failedToSend;
        });
    }

    protected synchronized void relay(ServerThread sender, String message) {
        if (!isRunning) return;

        String senderString = sender == null ? String.format("Room[%s]", getName()) : sender.getDisplayName();
        final long senderId = sender == null ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        final String formattedMessage = String.format("%s: %s", senderString, message);

        info(String.format("sending message to %s recipients: %s", clientsInRoom.size(), formattedMessage));

        clientsInRoom.values().removeIf(serverThread -> {
            boolean failedToSend = !serverThread.sendMessage(senderId, formattedMessage);
            if (failedToSend) {
                LoggerUtil.INSTANCE.warning(
                        String.format("Removing disconnected %s from list", serverThread.getDisplayName()));
                disconnect(serverThread);
            }
            return failedToSend;
        });
    }

    private synchronized void disconnect(ServerThread client) {
        if (!isRunning) return;
        ServerThread disconnectingServerThread = clientsInRoom.remove(client.getClientId());
        if (disconnectingServerThread != null) {
            clientsInRoom.values().removeIf(serverThread -> {
                if (serverThread.getClientId() == disconnectingServerThread.getClientId()) return true;
                boolean failedToSend = !serverThread.sendClientInfo(disconnectingServerThread.getClientId(),
                        disconnectingServerThread.getClientName(), RoomAction.LEAVE);
                if (failedToSend) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverThread.getDisplayName()));
                    disconnect(serverThread);
                }
                return failedToSend;
            });
            relay(null, disconnectingServerThread.getDisplayName() + " disconnected");
            disconnectingServerThread.disconnect();
        }
        autoCleanup();
    }

    protected synchronized void disconnectAll() {
        info("Disconnect All triggered");
        if (!isRunning) return;
        clientsInRoom.values().removeIf(client -> {
            disconnect(client);
            return true;
        });
        info("Disconnect All finished");
    }

    private void autoCleanup() {
        if (!Room.LOBBY.equalsIgnoreCase(name) && clientsInRoom.isEmpty()) {
            close();
        }
    }

    @Override
    public void close() {
        if (!clientsInRoom.isEmpty()) {
            relay(null, "Room is shutting down, migrating to lobby");
            info(String.format("migrating %s clients", clientsInRoom.size()));
            clientsInRoom.values().removeIf(client -> {
                try {
                    Server.INSTANCE.joinRoom(Room.LOBBY, client);
                } catch (RoomNotFoundException e) {
                    LoggerUtil.INSTANCE.severe("Lobby not found, this shouldn't happen", e);
                }
                return true;
            });
        }
        Server.INSTANCE.removeRoom(this);
        isRunning = false;
        clientsInRoom.clear();
        info("closed");
    }

    // Room-related handlers
    public void handleListRooms(ServerThread sender, String roomQuery) {
        sender.sendRooms(Server.INSTANCE.listRooms(roomQuery));
    }

    public void handleCreateRoom(ServerThread sender, String roomName) {
        try {
            Server.INSTANCE.createRoom(roomName);
            Server.INSTANCE.joinRoom(roomName, sender);
        } catch (RoomNotFoundException e) {
            LoggerUtil.INSTANCE.severe("Room wasn't found (shouldn't happen)", e);
        } catch (DuplicateRoomException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s already exists", roomName));
        }
    }

    public void handleJoinRoom(ServerThread sender, String roomName) {
        try {
            Server.INSTANCE.joinRoom(roomName, sender);
        } catch (RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s doesn't exist", roomName));
        }
    }

    protected synchronized void handleDisconnect(BaseServerThread sender) {
        handleDisconnect((ServerThread) sender);
    }

    protected synchronized void handleDisconnect(ServerThread sender) {
        disconnect(sender);
    }

    protected synchronized void handleReverseText(ServerThread sender, String text) {
        StringBuilder sb = new StringBuilder(text);
        sb.reverse();
        relay(sender, sb.toString());
    }

    protected synchronized void handleMessage(ServerThread sender, String text) {
        relay(sender, text);
    }
}
