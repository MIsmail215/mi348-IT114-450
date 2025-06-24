// UCID mi348 6/23/25
package M4.Part2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private int port = 3000;
    private Map<Integer, ServerThread> clients = new ConcurrentHashMap<>();
    private int clientIdCounter = 1;

    public synchronized int addClient(ServerThread client) {
        int id = clientIdCounter++;
        clients.put(id, client);
        return id;
    }

    public void removeClient(int id) {
        clients.remove(id);
    }

    public void pm(int fromId, int toId, String message) {
        ServerThread sender = clients.get(fromId);
        ServerThread receiver = clients.get(toId);
        if (receiver != null && sender != null) {
            String formatted = "PM from " + sender.getClientName() + ": " + message;
            receiver.sendMessage("Server: " + formatted);
            sender.sendMessage("Server: " + formatted);
        } else if (sender != null) {
            sender.sendMessage("Server: PM failed. User ID not found.");
        }
    }

    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ServerThread clientThread = new ServerThread(clientSocket, this);
                int id = addClient(clientThread);
                clientThread.setClientId(id);
                new Thread(clientThread).start();
                System.out.println("Client " + id + " connected.");
            }
        } catch (IOException e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {}
        server.start(port);
    }
}
