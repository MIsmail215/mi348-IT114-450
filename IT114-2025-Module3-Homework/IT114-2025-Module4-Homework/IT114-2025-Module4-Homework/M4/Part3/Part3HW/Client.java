
package M4.Part3.Part3HW;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import M4.Part3.Part3HW.Constants;


import M4.Part3.Part3HW.TextFX.Color;

public class Client {

    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final Pattern ipAddressPattern = Pattern
    .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");

    private volatile boolean isRunning = true;

    public Client() {
        System.out.println("Client Created");
    }

    public boolean isConnected() {
        if (server == null) return false;
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            System.out.println("Client connected");
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }
//UCID mi348 6/23/2025
    private boolean processClientCommand(String text) throws IOException {
        boolean wasCommand = false;
        if (isConnection(text)) {
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            wasCommand = true;
        } else if ("/quit".equalsIgnoreCase(text)) {
            close();
            wasCommand = true;
        } else if ("/disconnect".equalsIgnoreCase(text)) {
            String[] commandData = { Constants.COMMAND_TRIGGER, "disconnect" };
            sendToServer(String.join(",", commandData));
            wasCommand = true;
        } else if (text.startsWith("/reverse")) {
            text = text.replace("/reverse", "").trim();
            String[] commandData = { Constants.COMMAND_TRIGGER, "reverse", text };
            sendToServer(String.join(",", commandData));
            wasCommand = true;
        } else if (text.startsWith("/shuffle")) {
            text = text.replace("/shuffle", "").trim();
            String[] commandData = { Constants.COMMAND_TRIGGER, "shuffle", text };
            sendToServer(String.join(",", commandData));
            wasCommand = true;
        }
        return wasCommand;
    }

    public void start() throws IOException {
        System.out.println("Client starting");
        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);
        inputFuture.join();
    }

    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                String fromServer = (String) in.readObject();
                if (fromServer != null) {
                    System.out.println(TextFX.colorize(fromServer, Color.BLUE));
                } else {
                    System.out.println("Server disconnected");
                    break;
                }
            }
        } catch (ClassCastException | ClassNotFoundException | IOException e) {
            if (isRunning) {
                System.out.println("Connection dropped");
                e.printStackTrace();
            }
        } finally {
            closeServerConnection();
        }
        System.out.println("listenToServer thread stopped");
    }

    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            System.out.println("Waiting for input");
            while (isRunning) {
                String userInput = si.nextLine();
                if (!processClientCommand(userInput)) {
                    sendToServer(userInput);
                }
            }
        } catch (IOException ioException) {
            System.out.println("Error in listenToInput()");
            ioException.printStackTrace();
        }
        System.out.println("listenToInput thread stopped");
    }

    private void sendToServer(String message) throws IOException {
        if (isConnected()) {
            out.writeObject(message);
            out.flush();
        } else {
            System.out.println("Not connected to server.");
        }
    }

    private void close() {
        isRunning = false;
        closeServerConnection();
        System.out.println("Client terminated");
    }

    private void closeServerConnection() {
        try { if (out != null) out.close(); } catch (Exception e) { e.printStackTrace(); }
        try { if (in != null) in.close(); } catch (Exception e) { e.printStackTrace(); }
        try { if (server != null) server.close(); } catch (IOException e) { e.printStackTrace(); }
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
