package Project.Server;

import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import Project.Common.TextFX.Color;
import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.RoomAction;
import Project.Common.RoomResultPayload;
import Project.Common.TextFX;
import Project.Server.Room;
import Project.Common.RoomResultPayload;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends BaseServerThread {
    private Consumer<ServerThread> onInitializationComplete;

    // Logs thread information with client id
    @Override
    protected void info(String message) {
        LoggerUtil.INSTANCE
            .info(TextFX.colorize(String.format("Thread[%s]: %s", this.getClientId(), message), Color.CYAN));
    }

    // Constructor sets client socket and initialization callback
    protected ServerThread(Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        this.client = myClient;
        this.onInitializationComplete = onInitializationComplete;
        info("ServerThread created");
    }

    // Sends the list of available rooms to the client
    public boolean sendRooms(List<String> rooms) {
        RoomResultPayload rrp = new RoomResultPayload();
        rrp.setRooms(rooms);
        return sendToClient(rrp);
    }

    // Sends a disconnect payload
    protected boolean sendDisconnect(long clientId) {
        Payload payload = new Payload();
        payload.setClientId(clientId);
        payload.setPayloadType(PayloadType.DISCONNECT);
        return sendToClient(payload);
    }

    // Resets the user list on the client side
    protected boolean sendResetUserList() {
        return sendClientInfo(Constants.DEFAULT_CLIENT_ID, null, RoomAction.JOIN);
    }

    // Sends client info (join/leave) to another client
    protected boolean sendClientInfo(long clientId, String clientName, RoomAction action) {
        return sendClientInfo(clientId, clientName, action, false);
    }

    // Sends client info with sync flag for silent updates
    protected boolean sendClientInfo(long clientId, String clientName, RoomAction action, boolean isSync) {
        ConnectionPayload payload = new ConnectionPayload();
        switch (action) {
            case JOIN:
                payload.setPayloadType(PayloadType.ROOM_JOIN);
                break;
            case LEAVE:
                payload.setPayloadType(PayloadType.ROOM_LEAVE);
                break;
            default:
                break;
        }
        if (isSync) {
            payload.setPayloadType(PayloadType.SYNC_CLIENT);
        }
        payload.setClientId(clientId);
        payload.setClientName(clientName);
        return sendToClient(payload);
    }

    // Sends the assigned client ID to the client
    protected boolean sendClientId() {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setPayloadType(PayloadType.CLIENT_ID);
        payload.setClientId(getClientId());
        payload.setClientName(getClientName());
        return sendToClient(payload);
    }

    // Sends a message payload
    protected boolean sendMessage(long clientId, String message) {
    Payload payload = new Payload();
    payload.setPayloadType(PayloadType.MESSAGE);
    payload.setMessage("[" + getClientName() + "]: " + message); // Use name in message
    payload.setClientId(clientId);
    return sendToClient(payload);
}

    // Handles all incoming payloads from client
    @Override
    protected void processPayload(Payload incoming) {
        switch (incoming.getPayloadType()) {
            case CLIENT_CONNECT:
                setClientName(((ConnectionPayload) incoming).getClientName().trim());
                break;

            case DISCONNECT:
                currentRoom.handleDisconnect(this);
                break;

            case MESSAGE:
                currentRoom.handleMessage(this, incoming.getMessage());
                break;

            case REVERSE:
                currentRoom.handleReverseText(this, incoming.getMessage());
                break;

            case ROOM_CREATE:
                currentRoom.handleCreateRoom(this, incoming.getMessage());
                break;

            case ROOM_JOIN:
                currentRoom.handleJoinRoom(this, incoming.getMessage());
                break;

            case ROOM_LEAVE:
                currentRoom.handleJoinRoom(this, Room.LOBBY);
                break;

            case ROOM_LIST:
                currentRoom.handleListRooms(this, incoming.getMessage());
                break;

            // ==== MILESTONE 2 PAYLOADS ====
            case GAME_READY:
                currentRoom.handleGameReady(this); // Notifies that player is ready
                break;

            case GAME_PICK:
                currentRoom.handlePlayerPick(this, incoming.getMessage()); // Sends the pick made by the player
                break;

            default:
                LoggerUtil.INSTANCE.warning(TextFX.colorize("Unknown payload type received", Color.RED));
                break;
        }
    }

    // Called once the thread is fully initialized and ready
    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this);
    }
}
