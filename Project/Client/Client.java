
// UCID: mi348
// Date: 2025-08-03
package Project.Client;

import Project.Common.*;
import Project.Common.TextFX.Color;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
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
            case CREATE: payload.setPayloadType(PayloadType.ROOM_CREATE); break;
            case JOIN: payload.setPayloadType(PayloadType.ROOM_JOIN); break;
            case LEAVE: payload.setPayloadType(PayloadType.ROOM_LEAVE); break;
            case LIST: payload.setPayloadType(PayloadType.ROOM_LIST); break;
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
                    LoggerUtil.INSTANCE.info(TextFX.colorize(fromServer.getMessage(), Color.GREEN));
                }
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.warning("Connection dropped: " + e.getMessage());
        }
    }

    public void startGUI() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rock Paper Scissors - Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(3, 2));

            JButton listRoomsButton = new JButton("List Rooms");
            JButton createRoomButton = new JButton("Create Room");
            JButton readyButton = new JButton("Ready");
            JButton rockButton = new JButton("Rock");
            JButton paperButton = new JButton("Paper");
            JButton scissorsButton = new JButton("Scissors");

            panel.add(listRoomsButton);
            panel.add(createRoomButton);
            panel.add(readyButton);
            panel.add(rockButton);
            panel.add(paperButton);
            panel.add(scissorsButton);

            frame.getContentPane().add(panel);
            frame.setVisible(true);

            listRoomsButton.addActionListener(e -> {
                try { processClientCommand("/listrooms"); } catch (IOException ignored) {}
            });
            createRoomButton.addActionListener(e -> {
                try { processClientCommand("/createroom MyRoom"); } catch (IOException ignored) {}
            });
            readyButton.addActionListener(e -> {
                try { processClientCommand("/ready"); } catch (IOException ignored) {}
            });
            rockButton.addActionListener(e -> {
                try { processClientCommand("/pick rock"); } catch (IOException ignored) {}
            });
            paperButton.addActionListener(e -> {
                try { processClientCommand("/pick paper"); } catch (IOException ignored) {}
            });
            scissorsButton.addActionListener(e -> {
                try { processClientCommand("/pick scissors"); } catch (IOException ignored) {}
            });
        });
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        client.startGUI(); // Start the Swing GUI
    }
}
