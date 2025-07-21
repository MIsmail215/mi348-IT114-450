// Updated: 2025-07-20 | UCID: Mi348
package Project.Server;

import Project.Server.ServerThread;

public class PlayerState {
    private final ServerThread client;
    private String pick;
    private int points = 0;
    private boolean eliminated = false;
    private boolean ready = false;

    public PlayerState(ServerThread client) {
        this.client = client;
    }

    public long getId() {
        return client.getClientId();
    }

    public String getName() {
        return client.getClientName();
    }

    public String getPick() {
        return pick;
    }

    public void setPick(String pick) {
        this.pick = pick;
    }

    public int getPoints() {
        return points;
    }

    public void addPoint() {
        points++;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
