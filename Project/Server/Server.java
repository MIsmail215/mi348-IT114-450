package Project.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import Project.Common.LoggerUtil;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;
import Project.Exceptions.DuplicateRoomException;
import Project.Exceptions.RoomNotFoundException;

public enum Server {
    INSTANCE; // Singleton instance

    private int port = 3000;
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private boolean isRunning = true;
    private long nextClientId = 0;

    private void info(String message) {
        LoggerUtil.INSTANCE.info(TextFX.colorize(String.format("Server: %s", message), Color.YELLOW)); //UCID:Mi348
    }

    private Server() {
        //UCID:Mi348 - Hook to cleanup on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            info("JVM is shutting down. Perform cleanup tasks."); //UCID:Mi348
            shutdown();
        }));
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
//UCID MI348 
    private void start(int port) {
        this.port = port;
        info("Listening on port " + this.port); //UCID:Mi348 
        try (ServerSocket serverSocket = new ServerSocket(port)) { //Opens the server on the given port.
            createRoom(Room.LOBBY); //UCID:Mi348 - Create default lobby
            while (isRunning) {
                info("Waiting for next client"); // Logs waiting state
                Socket incomingClient = serverSocket.accept(); // blocks until client connects
                info("Client connected"); //UCID:Mi348
                ServerThread serverThread = new ServerThread(incomingClient, this::onServerThreadInitialized);
                serverThread.start();
            }
        } catch (DuplicateRoomException e) {
            LoggerUtil.INSTANCE.severe(TextFX.colorize("Lobby already exists (this shouldn't happen)", Color.RED)); //UCID:Mi348
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe(TextFX.colorize("Error accepting connection", Color.RED), e); 
        } finally {
            info("Closing server socket"); 
        }
    }

    private void onServerThreadInitialized(ServerThread serverThread) {
        nextClientId = Math.max(++nextClientId, 1);
        serverThread.setClientId(nextClientId);
        serverThread.sendClientId();// Send ID to client
        info(String.format("*%s initialized*", serverThread.getDisplayName())); //UCID:Mi348
        try {
            joinRoom(Room.LOBBY, serverThread);
            info(String.format("*%s added to Lobby*", serverThread.getDisplayName())); //UCID:Mi348
        } catch (RoomNotFoundException e) {
            info(String.format("*Error adding %s to Lobby*", serverThread.getDisplayName())); //UCID:Mi348
            e.printStackTrace();
        }
    }

    protected void createRoom(String name) throws DuplicateRoomException {
        final String nameCheck = name.toLowerCase();
        if (rooms.containsKey(nameCheck)) {
            throw new DuplicateRoomException(String.format("Room %s already exists", name));
        }
        Room room = new Room(name);
        rooms.put(nameCheck, room);
        info(String.format("Created new Room %s", name)); //UCID:Mi348
    }

    protected void joinRoom(String name, ServerThread client) throws RoomNotFoundException {
        final String nameCheck = name.toLowerCase();
        if (!rooms.containsKey(nameCheck)) {
            throw new RoomNotFoundException(String.format("Room %s wasn't found", name));
        }
        Room currentRoom = client.getCurrentRoom();
        if (currentRoom != null) {
            info("Removing client from previous Room " + currentRoom.getName()); //UCID:Mi348
            currentRoom.removeClient(client);
        }
        Room next = rooms.get(nameCheck);
        next.addClient(client);
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
        info(String.format("Removed room %s", room.getName())); //UCID:Mi348
    }

    private synchronized void relayToAllRooms(ServerThread sender, String message) {
        String senderString = sender == null ? "Server" : sender.getDisplayName();
        final String formattedMessage = String.format("%s: %s", senderString, message);

        rooms.values().forEach(room -> {
            room.relay(sender, formattedMessage);
        });
    }

    public synchronized void broadcastMessageToAllRooms(ServerThread sender, String message) {
        relayToAllRooms(sender, message);
    }

    public static void main(String[] args) {
        //UCID:Mi348 - Logger setup
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("server.log");
        LoggerUtil.INSTANCE.setConfig(config); // Must come first!

        LoggerUtil.INSTANCE.info("Server Starting"); //UCID:Mi348

        Server server = Server.INSTANCE;
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // use default 3000
        }
        server.start(port);
        LoggerUtil.INSTANCE.warning("Server Stopped"); //UCID:Mi348
    }
}
