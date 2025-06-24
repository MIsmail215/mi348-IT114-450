package M4.Part3.Part3HW;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import M4.Part3.Part3HW.TextFX.Color;

public class ServerThread extends Thread {
    private Socket client;
    private boolean isRunning = false;
    private ObjectOutputStream out;
    private Server server;
    private long clientId;
    private Consumer<ServerThread> onInitializationComplete;

    public ServerThread(Socket myClient, Server server, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(server, "Server cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        this.client = myClient;
        this.server = server;
        this.clientId = this.getId();
        this.onInitializationComplete = onInitializationComplete;
        info("ServerThread created");
    }

    public long getClientId() {
        return this.clientId;
    }

    public boolean isRunning() {
        return isRunning;
    }

    protected void disconnect() {
        if (!isRunning) return;
        info("Thread being disconnected by server");
        isRunning = false;
        this.interrupt();
        cleanup();
    }

    protected boolean sendToClient(String message) {
        if (!isRunning) return false;
        try {
            out.writeObject(message);
            out.flush();
            return true;
        } catch (IOException e) {
            info("Error sending message to client (likely disconnected)");
            cleanup();
            return false;
        }
    }

    @Override
    public void run() {
        info("Thread starting");
        ObjectInputStream in = null;
        try {
            this.out = new ObjectOutputStream(client.getOutputStream());
            this.out.flush(); // Important to flush header
            in = new ObjectInputStream(client.getInputStream());

            isRunning = true;
            onInitializationComplete.accept(this); // Notify server this thread is ready

            String fromClient;
            while (isRunning) {
                try {
                    fromClient = (String) in.readObject();
                    if (fromClient == null) {
                        throw new IOException("Connection interrupted");
                    } else {
                        info(TextFX.colorize("Received from my client: " + fromClient, Color.CYAN));
                        processPayload(fromClient);
                    }
                } catch (ClassCastException | ClassNotFoundException cce) {
                    System.err.println("Error reading object as specified type: " + cce.getMessage());
                    cce.printStackTrace();
                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        info("Thread interrupted during read");
                        break;
                    }
                    info("IO exception while reading from client");
                    e.printStackTrace();
                    break;
                }
            }
        } catch (Exception e) {
            info("General Exception");
            e.printStackTrace();
            info("My Client disconnected");
        } finally {
            isRunning = false;
            info("Exited thread loop. Cleaning up connection");
            cleanup();
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                info("Error closing input stream");
            }
        }
    }

    private void processPayload(String incoming) {
        if (!processCommand(incoming)) {
            server.handleMessage(this, incoming);
        }
    }
//UCID mi348 6/23/2025

    private boolean processCommand(String message) {
        boolean wasCommand = false;

        if (message.startsWith(Constants.COMMAND_TRIGGER)) {
            String[] commandData = message.split(",");
            if (commandData.length >= 2) {
                final String command = commandData[1].trim();
                System.out.println(TextFX.colorize("Checking command: " + command, Color.YELLOW));

                switch (command) {
                    case "disconnect":
                        server.handleDisconnect(this);
                        wasCommand = true;
                        break;
                    case "reverse":
                        String revText = String.join(" ", Arrays.copyOfRange(commandData, 2, commandData.length));
                        server.handleReverseText(this, revText);
                        wasCommand = true;
                        break;
                    case "shuffle":
                        String shuffleText = String.join(" ", Arrays.copyOfRange(commandData, 2, commandData.length));
                        server.handleShuffleText(this, shuffleText);
                        wasCommand = true;
                        break;
                    default:
                        break;
                }
            }
        }

        return wasCommand;
    }

    private void cleanup() {
        info("ServerThread cleanup() start");
        try {
            client.close();
            info("Closed Server-side Socket");
        } catch (IOException e) {
            info("Client already closed");
        }
        info("ServerThread cleanup() end");
    }

    private void info(String message) {
        System.out.println(String.format("Thread[%s]: %s", this.getClientId(), message));
    }
}
