// UCID: Mi348 | Updated: 2025-07-20
package Project.Common;

import Project.Common.Payload;
import Project.Common.PayloadType;

// Used to sync a player's score to all clients
public class PointsPayload extends Payload {
    private long clientId;  // ID of the client whose score is being synced
    private String clientName; // Optional: Player's name
    private int points;     // Number of points the player has

    public PointsPayload() {
        setPayloadType(PayloadType.SCOREBOARD); // Mark this as a scoreboard update
    }

    // Setters and Getters
    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientName(String name) {
        this.clientName = name;
    }

    public String getClientName() {
        return clientName;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }

    // Debug-friendly string
    @Override
    public String toString() {
        return String.format("[PointsPayload] %s (ID: %d) has %d points", clientName, clientId, points);
    }
}
