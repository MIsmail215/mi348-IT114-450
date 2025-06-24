// UCID mi348 6/23/25

package M4.Part1;

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

    public ServerThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
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
            out.println("Welcome, " + clientName);

            String input;
            while ((input = in.readLine()) != null) {
                if (!processCommand(input.trim())) {
                    server.relay(clientName, input);
                }
            }
        } catch (IOException e) {
            System.out.println("Connection to " + clientName + " lost.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket for " + clientName);
            }
        }
    }

    private boolean processCommand(String command) {
        if ("/flip".equalsIgnoreCase(command)) {
            server.flipCoin(clientName);
            return true;
        }
        return false;
    }
}
