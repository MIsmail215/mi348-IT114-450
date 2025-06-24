// UCID mi348 6/23/25

package M4.Part1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    private int port = 3000;
    private final List<ServerThread> clients = Collections.synchronizedList(new ArrayList<>());
//MI348 6/23/2025
    public void relay(String from, String message) {
        synchronized (clients) {
            for (ServerThread client : clients) {
                client.sendMessage(from + ": " + message);
            }
        }
    }

    public void flipCoin(String senderName) {
        String result = Math.random() < 0.5 ? "Heads" : "Tails";
        relay("Server", senderName + " flipped a coin and got " + result);
    }

    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept();
                ServerThread thread = new ServerThread(client, this);
                clients.add(thread);
                new Thread(thread).start();
                System.out.println("Client connected");
            }
        } catch (IOException e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            System.out.println("Server shutting down.");
        }
    }

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // use default
        }
        server.start(port);
    }
}
