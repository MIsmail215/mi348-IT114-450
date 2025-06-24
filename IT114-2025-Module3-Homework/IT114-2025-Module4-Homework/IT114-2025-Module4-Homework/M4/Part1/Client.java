// UCID mi348 6/23/25

package M4.Part1;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    private Socket server = null;
    private PrintWriter out = null;
    final Pattern ipAddressPattern = Pattern.compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    private boolean isRunning = false;

    public Client() {
        System.out.println("Client Created");
    }

    public boolean isConnected() {
        return server != null && server.isConnected() && !server.isClosed()
                && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    private boolean connect(String address, int port, Scanner si) {
        try {
            server = new Socket(address, port);
            out = new PrintWriter(server.getOutputStream(), true);
            System.out.println("Client connected");

            new Thread(() -> {
                try (Scanner serverInput = new Scanner(server.getInputStream())) {
                    while (serverInput.hasNextLine()) {
                        System.out.println(serverInput.nextLine());
                    }
                } catch (IOException e) {
                    System.out.println("Lost connection to server.");
                }
            }).start();

            System.out.print("Enter your name: ");
            String name = si.nextLine();
            out.println(name);

        } catch (UnknownHostException e) {
            System.out.println("Unknown host.");
        } catch (IOException e) {
            System.out.println("Could not connect to server.");
        }
        return isConnected();
    }

    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }
//MI348 6/23/2025
    private boolean processClientCommand(String text, Scanner si) {
        if (isConnection(text)) {
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()), si);
            return true;
        } else if ("/quit".equalsIgnoreCase(text)) {
            isRunning = false;
            return true;
        }
        return false;
    }

    public void start() throws IOException {
        System.out.println("Client starting");
        try (Scanner si = new Scanner(System.in)) {
            String line = "";
            isRunning = true;
            while (isRunning) {
                System.out.print("> ");
                line = si.nextLine();
                if (!processClientCommand(line, si)) {
                    if (isConnected()) {
                        out.println(line);
                        if (out.checkError()) {
                            System.out.println("Connection to server may have been lost");
                        }
                    } else {
                        System.out.println("Not connected to server");
                    }
                }
            }
            System.out.println("Exited loop");
        } catch (Exception e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private void close() {
        try {
            if (out != null) out.close();
        } catch (Exception e) {
            System.out.println("Output stream was never opened.");
        }
        try {
            if (server != null) server.close();
        } catch (IOException e) {
            System.out.println("Error closing socket.");
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.start();
        } catch (IOException e) {
            System.out.println("Exception from main()");
            e.printStackTrace();
        }
    }
}
