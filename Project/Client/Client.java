// UCID: mi348
// Date: 2025-08-04
package Project.Client;

import Project.Common.*;
import Project.Common.TextFX.Color;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private JTextArea gameEventLog = new JTextArea();
    private JTextArea playerStatusArea = new JTextArea();

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

    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    public boolean processClientCommand(String text) throws IOException {
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
            } else if (text.equalsIgnoreCase(Command.LIST_ROOMS.command)) {
                sendRoomAction("", RoomAction.LIST);
                wasCommand = true;
            } else if (text.equalsIgnoreCase(Command.READY.command)) {
                sendGameReady();
                wasCommand = true;
            } else if (text.startsWith(Command.CREATE_ROOM.command)) {
                text = text.replace(Command.CREATE_ROOM.command, "").trim();
                sendRoomAction(text, RoomAction.CREATE);
                wasCommand = true;
            } else if (text.startsWith(Command.PICK.command)) {
                text = text.replace(Command.PICK.command, "").trim();
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
            case CREATE -> payload.setPayloadType(PayloadType.ROOM_CREATE);
            case JOIN -> payload.setPayloadType(PayloadType.ROOM_JOIN);
            case LEAVE -> payload.setPayloadType(PayloadType.ROOM_LEAVE);
            case LIST -> payload.setPayloadType(PayloadType.ROOM_LIST);
        }
        sendToServer(payload);
    }

    private void sendGameReady() throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.GAME_READY);
        sendToServer(payload);
    }

    private void sendGamePick(String choice) throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.GAME_PICK);
        payload.setMessage(choice);
        sendToServer(payload);
    }

    private void sendClientName(String name) throws IOException {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setClientName(name);
        payload.setPayloadType(PayloadType.CLIENT_CONNECT);
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

    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject();
                if (fromServer != null) {
                    String msg = fromServer.getMessage();
                    if (msg != null && !msg.isBlank()) {
                        LoggerUtil.INSTANCE.info(TextFX.colorize(msg, Color.GREEN));
                        SwingUtilities.invokeLater(() -> gameEventLog.append(msg + "\n"));
                    }

                    if (fromServer.getPayloadType() == PayloadType.GAME_RESULT) {
                        if (fromServer instanceof RoomResultPayload resultPayload) {
                            updateGameResults(resultPayload);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.warning("Connection dropped: " + e.getMessage());
        }
    }

    private void updateGameResults(RoomResultPayload resultPayload) {
        StringBuilder eventBuilder = new StringBuilder();
        StringBuilder statusBuilder = new StringBuilder();

        eventBuilder.append("Round result:\n");
        eventBuilder.append("Winner: ").append(resultPayload.getWinnerName()).append("\n");

        Map<User, String> choices = resultPayload.getPlayerChoices();
        Map<User, Integer> points = resultPayload.getPlayerPoints();
        Set<User> eliminated = resultPayload.getEliminatedPlayers();

        eventBuilder.append("Choices:\n");
        for (User user : choices.keySet()) {
            eventBuilder.append("- ").append(user.getClientName())
                        .append(": ").append(choices.get(user)).append("\n");
        }

        statusBuilder.append("Player Status:\n");
        for (User user : points.keySet()) {
            String name = user.getClientName();
            int point = points.get(user);
            String state = eliminated.contains(user) ? "eliminated"
                            : choices.containsKey(user) ? "picked"
                            : "waiting";
            statusBuilder.append(String.format("- %s | %d pts | %s\n", name, point, state));
        }

        SwingUtilities.invokeLater(() -> {
            gameEventLog.append(eventBuilder.toString() + "\n");
            playerStatusArea.setText(statusBuilder.toString());
        });
    }

    public void startGUI() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rock Paper Scissors Multiplayer - Milestone 3");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 600);

            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JTextField hostField = new JTextField("localhost", 10);
            JTextField portField = new JTextField("3000", 5);
            JTextField nameField = new JTextField("YourName", 10);
            JButton connectBtn = new JButton("Connect");

            topPanel.add(new JLabel("Host:"));
            topPanel.add(hostField);
            topPanel.add(new JLabel("Port:"));
            topPanel.add(portField);
            topPanel.add(new JLabel("Username:"));
            topPanel.add(nameField);
            topPanel.add(connectBtn);

            gameEventLog.setEditable(false);
            gameEventLog.setBorder(BorderFactory.createTitledBorder("Game Events"));
            JScrollPane eventScroll = new JScrollPane(gameEventLog);
            eventScroll.setPreferredSize(new Dimension(500, 400));

            playerStatusArea.setEditable(false);
            playerStatusArea.setBorder(BorderFactory.createTitledBorder("Players"));
            JScrollPane playerScroll = new JScrollPane(playerStatusArea);
            playerScroll.setPreferredSize(new Dimension(250, 400));

            JPanel buttonPanel = new JPanel();
            JButton listRoomsButton = new JButton("Room List");
            JButton createRoomButton = new JButton("Create Room");
            JButton readyButton = new JButton("Ready");
            JButton rockButton = new JButton("Rock");
            JButton paperButton = new JButton("Paper");
            JButton scissorsButton = new JButton("Scissors");

            buttonPanel.add(listRoomsButton);
            buttonPanel.add(createRoomButton);
            buttonPanel.add(readyButton);
            buttonPanel.add(rockButton);
            buttonPanel.add(paperButton);
            buttonPanel.add(scissorsButton);

            frame.setLayout(new BorderLayout());
            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(eventScroll, BorderLayout.CENTER);
            frame.add(playerScroll, BorderLayout.WEST);
            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.setVisible(true);

            connectBtn.addActionListener(e -> {
                try {
                    String host = hostField.getText();
                    int port = Integer.parseInt(portField.getText());
                    String name = nameField.getText();
                    myUser.setClientName(name);
                    connect(host, port);
                    sendClientName(name);
                    gameEventLog.append("Connecting to " + host + ":" + port + " as " + name + "\n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            listRoomsButton.addActionListener(e -> {
                try {
                    processClientCommand("/listrooms");
                } catch (IOException ignored) {}
            });

            createRoomButton.addActionListener(e -> {
                try {
                    String roomName = JOptionPane.showInputDialog(frame, "Enter Room Name:");
                    if (roomName != null && !roomName.trim().isEmpty()) {
                        processClientCommand("/createroom " + roomName.trim());
                    }
                } catch (IOException ignored) {}
            });

            readyButton.addActionListener(e -> {
                try {
                    processClientCommand("/ready");
                } catch (IOException ignored) {}
            });

            ActionListener pickListener = e -> {
                try {
                    JButton btn = (JButton) e.getSource();
                    String pick = btn.getText().toLowerCase();
                    processClientCommand("/pick " + pick);
                    rockButton.setEnabled(false);
                    paperButton.setEnabled(false);
                    scissorsButton.setEnabled(false);
                } catch (IOException ignored) {}
            };

            rockButton.addActionListener(pickListener);
            paperButton.addActionListener(pickListener);
            scissorsButton.addActionListener(pickListener);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client.INSTANCE::startGUI);
    }
}
