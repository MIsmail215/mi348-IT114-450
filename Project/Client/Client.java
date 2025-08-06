// UCID: mi348
// Date: 2025-08-06
package Project.Client;

import Project.Common.*;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public enum Client {
    INSTANCE;
    
    private Timer roundTimer;
    private ClientGameUI ui;
    private PlayerTableModel playerTableModel;
    private final ConcurrentHashMap<Long, User> knownClients = new ConcurrentHashMap<>();
    private final User myUser = new User();
    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    private volatile boolean isRunning = true;
    private String lastKnownAddress = null;
    private int lastKnownPort = -1;
    private String lastPick = "";
    private boolean cooldownEnabled = false;

    private Client() {
        // Constructor is empty
    }
    
    public void start() {
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024);
        config.setFileCount(1);
        config.setLogLocation("client.log");
        LoggerUtil.INSTANCE.setConfig(config);
        LoggerUtil.INSTANCE.info("Client Created and Logger configured.");
    }
    
    public void registerUI(ClientGameUI ui) {
        this.ui = ui;
        this.playerTableModel = ui.getPlayerTableModel();
        attachListeners();
    }

    private void attachListeners() {
        ui.getConnectButton().addActionListener(e -> handleConnect(false));
        ui.getSpectatorButton().addActionListener(e -> handleConnect(true));
        ui.getReadyButton().addActionListener(e -> handleReady());
        ui.getAwayButton().addActionListener(e -> sendCommand("/toggleaway"));
        ui.getListRoomsButton().addActionListener(e -> sendCommand("/listrooms"));
        ui.getCreateRoomButton().addActionListener(e -> handleCreateRoom());
        ActionListener pickListener = e -> {
            JButton button = (JButton) e.getSource();
            sendPick(button.getText().toLowerCase());
        };
        ui.getRockButton().addActionListener(pickListener);
        ui.getPaperButton().addActionListener(pickListener);
        ui.getScissorsButton().addActionListener(pickListener);
        ui.getLizardButton().addActionListener(pickListener);
        ui.getSpockButton().addActionListener(pickListener);
    }

    private void handleConnect(boolean asSpectator) {
        if (isConnected()) {
            logToUI("Already connected.");
            return;
        }
        String host = ui.getHostField().getText().trim();
        String portStr = ui.getPortField().getText().trim();
        String name = ui.getNameField().getText().trim();
        if (host.isEmpty() || portStr.isEmpty() || name.isEmpty()) {
            logToUI("Host, Port, and Username cannot be empty.");
            return;
        }
        try {
            int port = Integer.parseInt(portStr);
            myUser.setClientName(name);
            myUser.setSpectator(asSpectator);
            String status = asSpectator ? " as a spectator" : "";
            logToUI("Connecting to " + host + ":" + port + " as " + name + status);
            if (connect(host, port)) {
                sendConnectionRequest(name, asSpectator);
            } else {
                logToUI("Connection failed. Check server and details.");
            }
        } catch (NumberFormatException ex) {
            logToUI("Invalid port number.");
        } catch (IOException ex) {
            logToUI("Error during connection: " + ex.getMessage());
        }
    }

    private void sendConnectionRequest(String name, boolean asSpectator) throws IOException {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setClientName(name);
        payload.setPayloadType(asSpectator ? PayloadType.JOIN_SPECTATOR : PayloadType.CLIENT_CONNECT);
        sendToServer(payload);
    }
    
    private void handleReady() {
        if (myUser.isSpectator()) return;
        
        ReadyPayload payload = new ReadyPayload();
        if (knownClients.size() <= 1 || knownClients.keySet().iterator().next() == myUser.getClientId()) {
            payload.setExtraOptionsEnabled(ui.getExtraOptionsCheck().isSelected());
            payload.setCooldownEnabled(ui.getCooldownCheck().isSelected());
        }
        
        try {
            sendToServer(payload);
            ui.getReadyButton().setEnabled(false);
        } catch (IOException e) {
            logToUI("Error sending ready status.");
        }
    }

    public boolean processClientCommand(String text) throws IOException {
        if (!text.startsWith(Constants.COMMAND_TRIGGER)) return false;
        String command = text.substring(1);
        String[] parts = command.split(" ", 2);
        String action = parts[0].toLowerCase();
        String argument = (parts.length > 1) ? parts[1] : "";
        switch (action) {
            case "ready": handleReady(); break;
            case "toggleaway": sendToggleAway(); break;
            case "pick": sendGamePick(argument); break;
            case "listrooms": sendRoomAction(argument, RoomAction.LIST); break;
            case "createroom": sendRoomAction(argument, RoomAction.CREATE); break;
            case "joinroom": sendRoomAction(argument, RoomAction.JOIN); break;
            default: return false;
        }
        return true;
    }

    private void sendToggleAway() throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.TOGGLE_AWAY);
        sendToServer(payload);
    }
    
    private void processPayload(Payload p) {
        SwingUtilities.invokeLater(() -> {
            String msg = p.getMessage();
            if (p.getPayloadType() != PayloadType.ROUND_START && msg != null && !msg.isBlank()) {
                logToUI(msg);
            }
            switch (p.getPayloadType()) {
                case CLIENT_ID:
                    if (p instanceof ConnectionPayload cp) {
                        myUser.setClientId(cp.getClientId());
                        knownClients.put(myUser.getClientId(), myUser);
                    }
                    updatePlayerList();
                    break;
                case ROOM_CLEAR:
                    knownClients.clear();
                    if (myUser.getClientId() != Constants.DEFAULT_CLIENT_ID) {
                        knownClients.put(myUser.getClientId(), myUser);
                    }
                    updatePlayerList();
                    break;
                case ROOM_JOIN:
                case SYNC_CLIENT:
                    if (p instanceof ConnectionPayload cp) {
                        if (cp.getClientId() == Constants.DEFAULT_CLIENT_ID || cp.getClientName() == null) {
                            return;
                        }
                        User user = knownClients.computeIfAbsent(cp.getClientId(), id -> new User());
                        user.setClientId(cp.getClientId());
                        user.setClientName(cp.getClientName());
                        user.setSpectator(cp.isSpectator());
                    }
                    updatePlayerList();
                    break;
                case ROOM_LEAVE:
                     if (p instanceof ConnectionPayload cp) {
                        knownClients.remove(cp.getClientId());
                     }
                    updatePlayerList();
                    break;
                case ROOM_LIST:
                    if (p instanceof RoomResultPayload rrp) {
                        new RoomListDialog(ui.getFrame(), rrp.getRooms()).setVisible(true);
                    }
                    break;
                case SYNC_POINTS:
                    if (p instanceof PointsPayload pp) {
                        User user = knownClients.get(pp.getClientId());
                        if (user != null) {
                            user.setPoints(pp.getPoints());
                            updatePlayerList();
                        }
                    }
                    break;
                case PLAYER_STATUS:
                    if (p instanceof PlayerStatusPayload psp) {
                        User user = knownClients.get(psp.getClientId());
                        if (user != null) {
                            user.setStatus(psp.getStatus());
                            updatePlayerList();
                        }
                    }
                    break;
                case ROUND_START:
                    if (p instanceof RoundStartPayload rsp) {
                        logToUI(rsp.getMessage());
                        startRoundTimer(rsp.getRoundDurationSeconds());
                    }
                    ui.getReadyButton().setEnabled(false);
                    ui.getExtraOptionsCheck().setEnabled(false);
                    ui.getCooldownCheck().setEnabled(false);
                    
                    // Re-enable all buttons first
                    ui.getRockButton().setEnabled(true);
                    ui.getPaperButton().setEnabled(true);
                    ui.getScissorsButton().setEnabled(true);
                    ui.getLizardButton().setEnabled(true);
                    ui.getSpockButton().setEnabled(true);

                    // Then, if cooldown is on, disable the last picked button
                    if (cooldownEnabled && lastPick != null && !lastPick.isEmpty()) {
                        switch (lastPick) {
                            case "rock": ui.getRockButton().setEnabled(false); break;
                            case "paper": ui.getPaperButton().setEnabled(false); break;
                            case "scissors": ui.getScissorsButton().setEnabled(false); break;
                            case "lizard": ui.getLizardButton().setEnabled(false); break;
                            case "spock": ui.getSpockButton().setEnabled(false); break;
                        }
                    }
                    break;
                
                // vvv THIS IS THE UPDATED LOGIC vvv
                case RESET_GAME_STATE:
                    ui.getReadyButton().setEnabled(true);
                    ui.getAwayButton().setSelected(false); // Un-toggle the away button
                    ui.getExtraOptionsCheck().setSelected(false);
                    ui.getExtraOptionsCheck().setEnabled(true);
                    ui.getCooldownCheck().setSelected(false);
                    ui.getCooldownCheck().setEnabled(true);
                    lastPick = ""; // Reset cooldown
                    // Re-enable all game buttons
                    ui.getRockButton().setEnabled(true);
                    ui.getPaperButton().setEnabled(true);
                    ui.getScissorsButton().setEnabled(true);
                    ui.getLizardButton().setEnabled(true);
                    ui.getSpockButton().setEnabled(true);
                    break;
                // ^^^ END OF UPDATE ^^^

                case SESSION_END:
                case GAME_RESULT:
                    if(roundTimer != null) {
                        roundTimer.stop();
                        ui.getTimerLabel().setText("Time Remaining: --");
                    }
                    // The main reset logic has been moved to RESET_GAME_STATE
                    break;
                default:
                    break;
            }
        });
    }
    private void startRoundTimer(int seconds) {
        if (roundTimer != null && roundTimer.isRunning()) {
            roundTimer.stop();
        }
        AtomicInteger timeLeft = new AtomicInteger(seconds);
        ui.getTimerLabel().setText("Time Remaining: " + timeLeft.get());
        roundTimer = new Timer(1000, e -> {
            int remaining = timeLeft.decrementAndGet();
            ui.getTimerLabel().setText("Time Remaining: " + remaining);
            if (remaining <= 0) {
                ((Timer)e.getSource()).stop();
                logToUI("Time is up!");
                ui.getRockButton().setEnabled(false);
                ui.getPaperButton().setEnabled(false);
                ui.getScissorsButton().setEnabled(false);
                ui.getLizardButton().setEnabled(false);
                ui.getSpockButton().setEnabled(false);
            }
        });
        roundTimer.start();
    }
    private void updatePlayerList() {
        List<User> currentUsers = new ArrayList<>();
        for (User user : knownClients.values()) {
            if (user != null && user.getClientName() != null) {
                currentUsers.add(user);
            }
        }
        playerTableModel.setPlayers(currentUsers);
    }
    private void handleCreateRoom() {
        String roomName = JOptionPane.showInputDialog(ui.getFrame(), "Enter new room name:", "Create Room", JOptionPane.PLAIN_MESSAGE);
        if (roomName != null && !roomName.trim().isEmpty()) {
            sendCommand("/createroom " + roomName.trim());
        }
    }
    private void sendPick(String pick) {
        if (myUser.isSpectator()) {
            logToUI("Spectators can't play!");
            return;
        }
        if (cooldownEnabled && pick.equals(lastPick)) {
            logToUI("Cooldown is enabled: Can't pick the same option twice in a row!");
            return;
        }
        
        // Disable all buttons immediately after picking
        ui.getRockButton().setEnabled(false);
        ui.getPaperButton().setEnabled(false);
        ui.getScissorsButton().setEnabled(false);
        ui.getLizardButton().setEnabled(false);
        ui.getSpockButton().setEnabled(false);
        
        lastPick = pick;
        logToUI("Picked " + pick.toUpperCase());
        sendCommand("/pick " + pick);
    }
    public void sendCommand(String command) {
        try {
            processClientCommand(command);
        } catch (IOException e) {
            logToUI("Error sending command: " + e.getMessage());
        }
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
            return false;
        }
        return isConnected();
    }
    private void listenToServer() {
        try {
            while (isRunning && isRunning) {
                Payload fromServer = (Payload) in.readObject();
                if (fromServer != null) {
                    processPayload(fromServer);
                }
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.warning("Connection dropped: " + e.getMessage());
            logToUI("Connection to server lost.");
        }
    }
    private void logToUI(String message) {
        ui.getEventLog().append(message + "\n");
        ui.getEventLog().setCaretPosition(ui.getEventLog().getDocument().getLength());
    }
    private void sendToServer(Payload payload) throws IOException {
        if (isConnected()) {
            out.writeObject(payload);
            out.flush();
        } else {
            logToUI("Not connected to server");
        }
    }
    private void sendGamePick(String choice) throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.GAME_PICK);
        payload.setMessage(choice);
        sendToServer(payload);
    }
     private void sendRoomAction(String roomName, RoomAction action) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(roomName);
        switch (action) {
            case CREATE: payload.setPayloadType(PayloadType.ROOM_CREATE); break;
            case JOIN: payload.setPayloadType(PayloadType.ROOM_JOIN); break;
            case LIST: payload.setPayloadType(PayloadType.ROOM_LIST); break;
            case LEAVE: payload.setPayloadType(PayloadType.ROOM_LEAVE); break;
        }
        sendToServer(payload);
    }
}