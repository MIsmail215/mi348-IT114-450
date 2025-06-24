// UCID mi348 6/23/25
package M4.Part2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerThread implements Runnable {
    private final Socket socket;
    private final Server server;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName = "Client";
    private int clientId;

    public ServerThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public void setClientId(int id) {
        this.clientId = id;
    }

    public int getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Enter your name:");
            clientName = in.readLine();
            out.println("Welcome, " + clientName + "! Your ID is " + clientId);

            String input;
            while ((input = in.readLine()) != null) {
                processCommand(input.trim());
            }
        } catch (IOException e) {
            System.out.println("Connection to client " + clientId + " lost.");
        } finally {
            server.removeClient(clientId);
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket for client " + clientId);
            }
        }
    }

    private void processCommand(String input) {
        if (input.startsWith("/pm")) {
            String[] parts = input.split(" ", 3);
            if (parts.length >= 3) {
                try {
                    int targetId = Integer.parseInt(parts[1]);
                    String message = parts[2];
                    server.pm(clientId, targetId, message);
                } catch (NumberFormatException e) {
                    sendMessage("Server: Invalid target ID format.");
                }
            } else {
                sendMessage("Server: Invalid /pm usage. Use /pm <id> <message>");
            }
        } else {
            sendMessage("Server: Unknown command.");
        }
    }
}
