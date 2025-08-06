package Project.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import Project.Common.Payload;
import Project.Common.User;

public abstract class BaseServerThread extends Thread {
    protected boolean isRunning = false;
    protected ObjectOutputStream out;
    protected Socket client;
    private User user = new User();
    protected Room currentRoom;

    public User getUser() {
        return this.user;
    }
    
    // ... (rest of the file is the same as the previous turn, no new changes) ...
    protected Room getCurrentRoom() {
        return this.currentRoom;
    }
    protected void setCurrentRoom(Room room) {
        if (room == null) {
            throw new NullPointerException("Room argument can't be null");
        }
        if (room == currentRoom) {
            System.out.println(
                    String.format("ServerThread set to the same room [%s], was this intentional?", room.getName()));
        }
        currentRoom = room;
    }
    public boolean isRunning() {
        return isRunning;
    }
    public void setClientId(long clientId) {
        this.user.setClientId(clientId);
    }
    public long getClientId() {
        return this.user.getClientId();
    }
    protected void setClientName(String clientName) {
        this.user.setClientName(clientName);
        onInitialized();
    }
    public String getClientName() {
        return this.user.getClientName();
    }
    public String getDisplayName() {
        return this.user.getDisplayName();
    }
    protected abstract void info(String message);
    protected abstract void onInitialized();
    protected abstract void processPayload(Payload payload);
    protected boolean sendToClient(Payload payload) {
        if (!isRunning) {
            return true;
        }
        try {
            info("Sending to client: " + payload);
            out.writeObject(payload);
            out.flush();
            return true;
        } catch (IOException e) {
            info("Error sending message to client (most likely disconnected)");
            cleanup();
            return false;
        }
    }
    protected void disconnect() {
        if (!isRunning) {
            return;
        }
        info("Thread being disconnected by server");
        isRunning = false;
        this.interrupt();
        cleanup();
    }
    @Override
    public void run() {
        info("Thread starting");
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
            this.out = out;
            isRunning = true;
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    if (getClientName() == null || getClientName().isBlank()) {
                        info("Client name not received. Disconnecting");
                        disconnect();
                    }
                }
            }, 3000);
            Payload fromClient;
            while (isRunning) {
                try {
                    fromClient = (Payload) in.readObject();
                    if (fromClient != null) {
                        info("Received from my client: " + fromClient);
                        processPayload(fromClient);
                    } else {
                        throw new IOException("Connection interrupted");
                    }
                } catch (ClassNotFoundException | ClassCastException cce) {
                    System.err.println("Error reading object as specified type: " + cce.getMessage());
                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        info("Thread interrupted during read");
                        break;
                    }
                    info("IO exception while reading from client");
                    break;
                }
            }
        } catch (Exception e) {
            info("My Client disconnected");
        } finally {
            if (currentRoom != null) {
                currentRoom.handleDisconnect((ServerThread) this);
            }
            isRunning = false;
            info("Exited thread loop. Cleaning up connection");
            cleanup();
        }
    }
    protected void cleanup() {
        info("ServerThread cleanup() start");
        try {
            currentRoom = null;
            if (out != null) out.close();
            if (client != null) client.close();
            user.reset();
            info("Closed Server-side Socket");
        } catch (Exception e) {
            info("Error during cleanup: " + e.getMessage());
        }
        info("ServerThread cleanup() end");
    }
}