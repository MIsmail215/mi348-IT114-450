package Project.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Project.Common.*;
import Project.Common.TextFX.Color;

public enum Client {
    INSTANCE;

    {
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024);
        config.setFileCount(1);
        config.setLogLocation("client.log");
        LoggerUtil.INSTANCE.setConfig(config);
    }

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    private volatile boolean isRunning = true;
    private final ConcurrentHashMap<Long, User> knownClients = new ConcurrentHashMap<>();
    private User myUser = new User();
    private String lastKnownAddress = null;
    private int lastKnownPort = -1;
    private final Pattern ipAddressPattern = Pattern.compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    private final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");

    private Client() {
        LoggerUtil.INSTANCE.info("Client Created");
    }

    public boolean isConnected() {
        return server != null && server.isConnected() && !server.isClosed()
                && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    private boolean connect(String address, int port) {
        try {
            this.lastKnownAddress = address;
            this.lastKnownPort = port;
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            CompletableFuture.runAsync(this::listenToServer);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.warning("Failed to connect: " + e.getMessage());
        }
        return isConnected();
    }

    private void attemptReconnect() {
        int retries = 0;
        while (retries < 5 && !isConnected()) {
            try {
                LoggerUtil.INSTANCE.info("Attempting to reconnect...");
                connect(lastKnownAddress, lastKnownPort);
                if (isConnected() && myUser.getClientName() != null) {
                    sendClientName(myUser.getClientName());
                    LoggerUtil.INSTANCE.info("Reconnected successfully");
                    break;
                }
            } catch (Exception e) {
                LoggerUtil.INSTANCE.warning("Reconnect failed: " + e.getMessage());
            }
            retries++;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    private boolean processClientCommand(String text) throws IOException {
        boolean wasCommand = false;
        if (text.startsWith(Constants.COMMAND_TRIGGER)) {
            text = text.substring(1);
            if (isConnection("/" + text)) {
                if (myUser.getClientName() == null || myUser.getClientName().isEmpty()) {
                    LoggerUtil.INSTANCE.warning(TextFX.colorize("Please set your name via /name <name> before connecting", Color.RED));
                    return true;
                }
                String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
                connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                sendClientName(myUser.getClientName());
                wasCommand = true;
            } else if (text.startsWith(Command.NAME.command)) {
                text = text.replace(Command.NAME.command, "").trim();
                if (text.length() == 0) {
                    LoggerUtil.INSTANCE.warning(TextFX.colorize("This command requires a name as an argument", Color.RED));
                    return true;
                }
                myUser.setClientName(text);
                LoggerUtil.INSTANCE.info(TextFX.colorize(String.format("Name set to %s", myUser.getClientName()), Color.YELLOW));
                wasCommand = true;
            } else if (text.equalsIgnoreCase(Command.LIST_USERS.command)) {
                LoggerUtil.INSTANCE.info(TextFX.colorize("Known clients:", Color.CYAN));
                knownClients.forEach((key, value) -> {
                    LoggerUtil.INSTANCE.info(TextFX.colorize(String.format("%s%s", value.getDisplayName(),
                            key == myUser.getClientId() ? " (you)" : ""), Color.CYAN));
                });
                wasCommand = true;
            } else if (Command.QUIT.command.equalsIgnoreCase(text)) {
                close();
                wasCommand = true;
            } else if (Command.DISCONNECT.command.equalsIgnoreCase(text)) {
                sendDisconnect();
                wasCommand = true;
            } else if (text.startsWith(Command.REVERSE.command)) {
                text = text.replace(Command.REVERSE.command, "").trim();
                sendReverse(text);
                wasCommand = true;
            } else if (text.startsWith(Command.CREATE_ROOM.command)) {
                text = text.replace(Command.CREATE_ROOM.command, "").trim();
                if (text.length() == 0) {
                    LoggerUtil.INSTANCE.warning(TextFX.colorize("This command requires a room name as an argument", Color.RED));
                    return true;
                }
                sendRoomAction(text, RoomAction.CREATE);
                wasCommand = true;
            } else if (text.startsWith(Command.JOIN_ROOM.command)) {
                text = text.replace(Command.JOIN_ROOM.command, "").trim();
                if (text.length() == 0) {
                    LoggerUtil.INSTANCE.warning(TextFX.colorize("This command requires a room name as an argument", Color.RED));
                    return true;
                }
                sendRoomAction(text, RoomAction.JOIN);
                wasCommand = true;
            } else if (text.startsWith(Command.LEAVE_ROOM.command)) {
                sendRoomAction(text, RoomAction.LEAVE);
                wasCommand = true;
            } else if (text.startsWith(Command.LIST_ROOMS.command)) {
                text = text.replace(Command.LIST_ROOMS.command, "").trim();
                sendRoomAction(text, RoomAction.LIST);
                wasCommand = true;
            } else if (text.equalsIgnoreCase(Command.READY.command)) {
                sendGameReady();
                wasCommand = true;
            } else if (text.startsWith(Command.PICK.command)) {
                text = text.replace(Command.PICK.command, "").trim();
                if (text.isEmpty()) {
                    LoggerUtil.INSTANCE.warning(TextFX.colorize("Usage: /pick <option>", Color.RED));
                    return true;
                }
                sendGamePick(text);
                wasCommand = true;
            }
        }
        return wasCommand;
    }

    private void sendRoomAction(String roomName, RoomAction action) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(roomName);
        switch (action) {
            case CREATE:
                payload.setPayloadType(PayloadType.ROOM_CREATE);
                break;
            case JOIN:
                payload.setPayloadType(PayloadType.ROOM_JOIN);
                break;
            case LEAVE:
                payload.setPayloadType(PayloadType.ROOM_LEAVE);
                break;
            case LIST:
                payload.setPayloadType(PayloadType.ROOM_LIST);
                break;
        }
        sendToServer(payload);
    }

    private void sendReverse(String msg) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(msg);
        payload.setPayloadType(PayloadType.REVERSE);
        sendToServer(payload);
    }

    private void sendDisconnect() throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.DISCONNECT);
        sendToServer(payload);
    }

    private void sendMessage(String msg) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(msg);
        payload.setPayloadType(PayloadType.MESSAGE);
        sendToServer(payload);
    }

    private void sendClientName(String name) throws IOException {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setClientName(name);
        payload.setPayloadType(PayloadType.CLIENT_CONNECT);
        sendToServer(payload);
    }

    // Sends READY payload to notify player is ready
    private void sendGameReady() throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.GAME_READY);
        sendToServer(payload);
    }

    // Sends PICK payload with player's chosen move
    private void sendGamePick(String choice) throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.GAME_PICK);
        payload.setMessage(choice);
        sendToServer(payload);
    }

    private void sendToServer(Payload payload) throws IOException {
        if (isConnected()) {
            out.writeObject(payload);
            out.flush();
        } else {
            LoggerUtil.INSTANCE.warning("Not connected to server");
        }
    }

    public void start() throws IOException {
        LoggerUtil.INSTANCE.info("Client starting");
        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);
        inputFuture.join();
    }

    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject();
                if (fromServer != null) {
                    processPayload(fromServer);
                } else {
                    LoggerUtil.INSTANCE.info("Server disconnected");
                    break;
                }
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.warning("Connection dropped: " + e.getMessage());
        } finally {
            closeServerConnection();
            if (isRunning) {
                attemptReconnect();
            }
        }
        LoggerUtil.INSTANCE.info("listenToServer thread stopped");
    }

    private void processPayload(Payload p) {
        switch (p.getPayloadType()) {
            case CLIENT_CONNECT:
                break;
            case CLIENT_ID:
                processClientData(p);
                break;
            case DISCONNECT:
                processDisconnect(p);
                break;
            case MESSAGE:
                LoggerUtil.INSTANCE.info(TextFX.colorize(p.getMessage(), Color.BLUE));
                break;
            case REVERSE:
                LoggerUtil.INSTANCE.info(TextFX.colorize(p.getMessage(), Color.PURPLE));
                break;
            case ROOM_JOIN:
            case ROOM_LEAVE:
            case SYNC_CLIENT:
                processRoomAction(p);
                break;
            case ROOM_LIST:
                processRoomsList(p);
                break;
            case GAME_RESULT:
            case GAME_STATE:
            case ROUND_START:
            case ROUND_END:
            case PLAYER_ELIMINATED:
            case SCOREBOARD:
            case SESSION_START:
            case SESSION_END:
                LoggerUtil.INSTANCE.info(TextFX.colorize(p.getMessage(), Color.GREEN));
                break;
            default:
                LoggerUtil.INSTANCE.warning(TextFX.colorize("Unhandled payload type", Color.YELLOW));
        }
    }

    private void processClientData(Payload p) {
        if (myUser.getClientId() != Constants.DEFAULT_CLIENT_ID) {
            LoggerUtil.INSTANCE.warning(TextFX.colorize("Client ID already set", Color.YELLOW));
        }
        myUser.setClientId(p.getClientId());
        myUser.setClientName(((ConnectionPayload) p).getClientName());
        knownClients.put(myUser.getClientId(), myUser);
        LoggerUtil.INSTANCE.info(TextFX.colorize("Connected", Color.GREEN));
    }

    private void processDisconnect(Payload p) {
        if (p.getClientId() == myUser.getClientId()) {
            knownClients.clear();
            myUser.reset();
            LoggerUtil.INSTANCE.info(TextFX.colorize("You disconnected", Color.RED));
        } else {
            User u = knownClients.remove(p.getClientId());
            if (u != null) {
                LoggerUtil.INSTANCE.info(TextFX.colorize(u.getDisplayName() + " disconnected", Color.RED));
            }
        }
    }

    private void processRoomAction(Payload p) {
        if (!(p instanceof ConnectionPayload)) return;
        ConnectionPayload cp = (ConnectionPayload) p;
        if (cp.getClientId() == Constants.DEFAULT_CLIENT_ID) {
            knownClients.clear();
            return;
        }
        switch (cp.getPayloadType()) {
            case ROOM_LEAVE:
                knownClients.remove(cp.getClientId());
                if (cp.getMessage() != null)
                    LoggerUtil.INSTANCE.info(TextFX.colorize(cp.getMessage(), Color.YELLOW));
                break;
            case ROOM_JOIN:
                if (cp.getMessage() != null)
                    LoggerUtil.INSTANCE.info(TextFX.colorize(cp.getMessage(), Color.GREEN));
            case SYNC_CLIENT:
                if (!knownClients.containsKey(cp.getClientId())) {
                    User u = new User();
                    u.setClientId(cp.getClientId());
                    u.setClientName(cp.getClientName());
                    knownClients.put(cp.getClientId(), u);
                }
                break;
            default:
                LoggerUtil.INSTANCE.warning("Invalid payload for room action");
        }
    }

    private void processRoomsList(Payload p) {
        if (!(p instanceof RoomResultPayload)) return;
        List<String> rooms = ((RoomResultPayload) p).getRooms();
        if (rooms == null || rooms.isEmpty()) {
            LoggerUtil.INSTANCE.warning(TextFX.colorize("No rooms found", Color.RED));
            return;
        }
        LoggerUtil.INSTANCE.info(TextFX.colorize("Room Results:", Color.PURPLE));
        LoggerUtil.INSTANCE.info(String.join("\n", rooms));
    }

    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            LoggerUtil.INSTANCE.info("Waiting for input");
            while (isRunning) {
                String input = si.nextLine();
                if (!processClientCommand(input)) {
                    sendMessage(input);
                }
            }
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Error in listenToInput", e);
        }
    }

    private void close() {
        isRunning = false;
        closeServerConnection();
        LoggerUtil.INSTANCE.info("Client terminated");
    }

    private void closeServerConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (server != null) server.close();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Error closing connection", e);
        }
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
            client.start();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Exception in main()", e);
        }
    }
}
