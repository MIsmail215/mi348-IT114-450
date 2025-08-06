package Project.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import Project.Common.LoggerUtil;
import Project.Common.TextFX.Color;
import Project.Common.TextFX;
import Project.Exceptions.DuplicateRoomException;
import Project.Exceptions.RoomNotFoundException;

public enum Server {
    INSTANCE;

    private int port = 3000;
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private boolean isRunning = true;
    private long nextClientId = 0;

    private void info(String message) {
        LoggerUtil.INSTANCE.info(TextFX.colorize(String.format("Server: %s", message), Color.YELLOW));
    }

    private Server() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            info("JVM is shutting down. Perform cleanup tasks.");
            shutdown();
        }));
    }

    public void addSpectatorToLobby(ServerThread spectator) {
        try {
            Room lobby = rooms.get(Room.LOBBY);
            if (lobby != null) {
                lobby.addSpectator(spectator);
                info(String.format("*%s added to Lobby as a spectator*", spectator.getDisplayName()));
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Error adding spectator to lobby", e);
        }
    }

    private void shutdown() {
        try {
            rooms.values().removeIf(room -> {
                room.disconnectAll();
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void start(int port) {
        this.port = port;
        info("Listening on port " + this.port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            createRoom(Room.LOBBY);
            while (isRunning) {
                info("Waiting for next client");
                Socket incomingClient = serverSocket.accept();
                info("Client connected");
                ServerThread serverThread = new ServerThread(incomingClient, this::onServerThreadInitialized);
                serverThread.start();
            }
        } catch (DuplicateRoomException e) {
            LoggerUtil.INSTANCE.severe(TextFX.colorize("Lobby already exists (this shouldn't happen)", Color.RED));
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe(TextFX.colorize("Error accepting connection", Color.RED), e); 
        } finally {
            info("Closing server socket"); 
        }
    }
    private void onServerThreadInitialized(ServerThread serverThread) {
        nextClientId = Math.max(++nextClientId, 1);
        serverThread.setClientId(nextClientId);
        serverThread.sendClientId();
        info(String.format("*%s initialized*", serverThread.getDisplayName()));
        if (!serverThread.isSpectator()) {
            try {
                joinRoom(Room.LOBBY, serverThread);
                info(String.format("*%s added to Lobby*", serverThread.getDisplayName()));
            } catch (RoomNotFoundException e) {
                info(String.format("*Error adding %s to Lobby*", serverThread.getDisplayName()));
                e.printStackTrace();
            }
        }
    }
    protected void createRoom(String name) throws DuplicateRoomException {
        final String nameCheck = name.toLowerCase();
        if (rooms.containsKey(nameCheck)) {
            throw new DuplicateRoomException(String.format("Room %s already exists", name));
        }
        Room room = new Room(name);
        rooms.put(nameCheck, room);
        info(String.format("Created new Room %s", name));
    }
    protected void joinRoom(String name, ServerThread client) throws RoomNotFoundException {
        final String nameCheck = name.toLowerCase();
        if (!rooms.containsKey(nameCheck)) {
            throw new RoomNotFoundException(String.format("Room %s wasn't found", name));
        }
        Room currentRoom = client.getCurrentRoom();
        if (currentRoom != null) {
            info("Removing client from previous Room " + currentRoom.getName());
            currentRoom.removeClient(client);
        }
        Room next = rooms.get(nameCheck);
        if (client.isSpectator()) {
            next.addSpectator(client);
        } else {
            next.addClient(client);
        }
    }
    protected List<String> listRooms(String roomQuery) {
        final String nameCheck = roomQuery.toLowerCase();
        return rooms.values().stream()
                .filter(room -> room.getName().toLowerCase().contains(nameCheck))
                .map(room -> room.getName())
                .limit(10)
                .sorted()
                .collect(Collectors.toList());
    }
    protected void removeRoom(Room room) {
        rooms.remove(room.getName().toLowerCase());
        info(String.format("Removed room %s", room.getName()));
    }
    public static void main(String[] args) {
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024);
        config.setFileCount(1);
        config.setLogLocation("server.log");
        LoggerUtil.INSTANCE.setConfig(config);
        LoggerUtil.INSTANCE.info("Server Starting");
        Server server = Server.INSTANCE;
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // use default 3000
        }
        server.start(port);
        LoggerUtil.INSTANCE.warning("Server Stopped");
    }
}