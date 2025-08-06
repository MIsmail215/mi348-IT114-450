// UCID: mi3-it114-450
// Date: 2025-08-05
package Project.Common;

import java.util.*;

public class GameResultPayload extends Payload {
    private String winnerName;
    private Map<User, String> playerChoices = new HashMap<>();
    private Map<User, Integer> playerPoints = new HashMap<>();
    private Set<User> eliminatedPlayers = new HashSet<>();
    
    public GameResultPayload() {
        // This payload is ONLY for game results.
        this.setPayloadType(PayloadType.GAME_RESULT);
    }

    public String getWinnerName() {
        return this.winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public Map<User, String> getPlayerChoices() {
        return this.playerChoices;
    }

    public void setPlayerChoices(Map<User, String> playerChoices) {
        this.playerChoices = playerChoices;
    }

    public Map<User, Integer> getPlayerPoints() {
        return this.playerPoints;
    }

    public void setPlayerPoints(Map<User, Integer> playerPoints) {
        this.playerPoints = playerPoints;
    }

    public Set<User> getEliminatedPlayers() {
        return this.eliminatedPlayers;
    }

    public void setEliminatedPlayers(Set<User> eliminatedPlayers) {
        this.eliminatedPlayers = eliminatedPlayers;
    }

    @Override
    public String toString() {
        return super.toString()
                + "\nWinner: " + this.winnerName
                + "\nChoices: " + this.playerChoices
                + "\nPoints: " + this.playerPoints
                + "\nEliminated: " + this.eliminatedPlayers;
    }
}