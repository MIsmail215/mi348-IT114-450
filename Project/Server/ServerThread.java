package Project.Server;

import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import Project.Common.*;
import Project.Common.TextFX.Color;

public class ServerThread extends BaseServerThread {
    private Consumer<ServerThread> onInitializationComplete;
    private boolean isSpectator = false;

    public boolean isSpectator() {
        return isSpectator;
    }
    public void setSpectator(boolean spectator) {
        isSpectator = spectator;
    }

    @Override
    protected void info(String message) {
        LoggerUtil.INSTANCE.info(TextFX.colorize(String.format("Thread[%s]: %s", this.getClientId(), message), Color.CYAN));
    }

    protected ServerThread(Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        this.client = myClient;
        this.onInitializationComplete = onInitializationComplete;
        info("ServerThread created");
    }

    @Override
    protected void processPayload(Payload incoming) {
        switch (incoming.getPayloadType()) {
            case CLIENT_CONNECT:
                setClientName(((ConnectionPayload) incoming).getClientName().trim());
                break;
            case JOIN_SPECTATOR:
                setClientName(((ConnectionPayload) incoming).getClientName().trim());
                Server.INSTANCE.addSpectatorToLobby(this);
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
            case GAME_READY:
                currentRoom.handleGameReady(this, incoming);
                break;
            case TOGGLE_AWAY:
                currentRoom.handleToggleAway(this);
                break;
            case GAME_PICK:
                currentRoom.handlePlayerPick(this, incoming.getMessage());
                break;
            default:
                LoggerUtil.INSTANCE.warning(TextFX.colorize("Unknown payload type received", Color.RED));
                break;
        }
    }
    
    public boolean sendRooms(List<String> rooms) {
        RoomResultPayload payload = new RoomResultPayload();
        payload.setRooms(rooms);
        return sendToClient(payload);
    }
    protected boolean sendDisconnect(long clientId) {
        Payload payload = new Payload();
        payload.setClientId(clientId);
        payload.setPayloadType(PayloadType.DISCONNECT);
        return sendToClient(payload);
    }
    protected boolean sendResetUserList() {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.ROOM_CLEAR);
        return sendToClient(payload);
    }
    protected boolean sendClientInfo(long clientId, String clientName, RoomAction action, boolean isSync, boolean isSpectator) {
        ConnectionPayload payload = new ConnectionPayload();
        switch (action) {
            case JOIN:
                payload.setPayloadType(isSync ? PayloadType.SYNC_CLIENT : PayloadType.ROOM_JOIN);
                break;
            case LEAVE:
                payload.setPayloadType(PayloadType.ROOM_LEAVE);
                break;
        }
        payload.setClientId(clientId);
        payload.setClientName(clientName);
        payload.setSpectator(isSpectator);
        return sendToClient(payload);
    }
    protected boolean sendClientId() {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setPayloadType(PayloadType.CLIENT_ID);
        payload.setClientId(getClientId());
        payload.setClientName(getClientName());
        return sendToClient(payload);
    }
    protected boolean sendMessage(long clientId, String message) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.MESSAGE);
        payload.setMessage(message);
        payload.setClientId(clientId);
        return sendToClient(payload);
    }
    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this);
    }
}