package Project.Common;

public class ConnectionPayload extends Payload {
    private String clientName;
    private boolean isSpectator;

    public boolean isSpectator() {
        return isSpectator;
    }

    public void setSpectator(boolean spectator) {
        isSpectator = spectator;
    }
    
    public String getClientName() {
        return clientName;
    }
    
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public String toString() {
        return super.toString() +
                String.format(" ClientName: [%s]",
                        getClientName());
    }
}