package Project.Server;

import Project.Common.User;
import Project.Common.PlayerStatus;

public class PlayerState {
    private final ServerThread client;
    private String pick;
    private int points = 0;
    private boolean eliminated = false;
    private boolean ready = false;
    private boolean isAway = false;
    private boolean isSpectator = false;
    private PlayerStatus status = PlayerStatus.ACTIVE;

    public PlayerState(ServerThread client) {
        this.client = client;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isAway() { return isAway; }
    public void setAway(boolean away) { isAway = away; }

    public boolean isSpectator() { return isSpectator; }
    public void setSpectator(boolean spectator) {
        isSpectator = spectator;
        if (spectator) {
            this.status = PlayerStatus.SPECTATING;
        }
    }

    public User getUser() { return client.getUser(); }
    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }
    public long getId() { return client.getClientId(); }
    public String getName() { return client.getClientName(); }
    public String getPick() { return pick; }
    public void setPick(String pick) { this.pick = pick; }
    public int getPoints() { return points; }
    public void addPoint() { points++; }
    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
        if (eliminated) {
            this.status = PlayerStatus.ELIMINATED;
        }
    }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
}