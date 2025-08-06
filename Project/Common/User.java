package Project.Common;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private String clientName;
    private int points = 0;
    private PlayerStatus status = PlayerStatus.ACTIVE;
    private boolean isSpectator = false;

    public boolean isSpectator() {
        return isSpectator;
    }

    public void setSpectator(boolean spectator) {
        isSpectator = spectator;
        if (spectator) {
            this.status = PlayerStatus.SPECTATING;
        }
    }

    public long getClientId() { return clientId; }
    public void setClientId(long clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String username) { this.clientName = username; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }
    public String getDisplayName() { return String.format("%s#%s", this.clientName, this.clientId); }
    public void reset() {
        this.clientId = Constants.DEFAULT_CLIENT_ID;
        this.clientName = null;
        this.points = 0;
        this.status = PlayerStatus.ACTIVE;
        this.isSpectator = false;
    }
}