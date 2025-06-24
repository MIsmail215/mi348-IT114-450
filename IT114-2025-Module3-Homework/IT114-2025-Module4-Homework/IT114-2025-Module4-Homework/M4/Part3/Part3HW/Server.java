package M4.Part3.Part3HW;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class Server {
    private int port = 3000;
    private final ConcurrentHashMap<Long, ServerThread> connectedClients = new ConcurrentHashMap<>();
    private boolean isRunning = true;

    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (isRunning) {
                System.out.println("Waiting for next client");
                Socket incomingClient = serverSocket.accept(); // blocking
                System.out.println("Client connected");
                ServerThread serverThread = new ServerThread(incomingClient, this, this::onServerThreadInitialized);
                serverThread.start();
            }
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            System.out.println("Closing server socket");
        }
    }
//UCID mi348 6/23/2025
    private void onServerThreadInitialized(ServerThread serverThread) {
        connectedClients.put(serverThread.getClientId(), serverThread);
        relay(null, String.format("*User[%s] connected*", serverThread.getClientId()));
    }

    private synchronized void disconnect(ServerThread serverThread) {
        serverThread.disconnect();
        ServerThread removed = connectedClients.remove(serverThread.getClientId());
        if (removed != null) {
            relay(null, "User[" + removed.getClientId() + "] disconnected");
        }
    }

    private synchronized void relay(ServerThread sender, String message) {
        String senderString = sender == null ? "Server" : String.format("User[%s]", sender.getClientId());
        final String formattedMessage = String.format("%s: %s", senderString, message);

        connectedClients.values().removeIf(st -> {
            boolean failedToSend = !st.sendToClient(formattedMessage);
            if (failedToSend) {
                disconnect(st);
            }
            return failedToSend;
        });
    }

    protected synchronized void handleDisconnect(ServerThread sender) {
        disconnect(sender);
    }

    protected synchronized void handleReverseText(ServerThread sender, String text) {
        StringBuilder sb = new StringBuilder(text);
        sb.reverse();
        relay(sender, sb.toString());
    }

    protected synchronized void handleShuffleText(ServerThread sender, String text) {
        List<String> letters = new ArrayList<>();
        for (char c : text.toCharArray()) {
            letters.add(String.valueOf(c));
        }
        Collections.shuffle(letters);
        StringBuilder shuffled = new StringBuilder();
        for (String letter : letters) {
            shuffled.append(letter);
        }

        String result = String.format("Shuffled from User[%s]: %s", sender.getClientId(), shuffled.toString());
        relay(null, result);
    }

    protected synchronized void handleMessage(ServerThread sender, String text) {
        relay(sender, text);
    }

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // default port 3000
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
