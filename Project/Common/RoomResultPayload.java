// UCID: mi348
// Date: 2025-08-04
package Project.Common;

import java.util.*;

public class RoomResultPayload extends Payload {
    private String winnerName;
    private Map<User, String> playerChoices = new HashMap<>();
    private Map<User, Integer> playerPoints = new HashMap<>();
    private Set<User> eliminatedPlayers = new HashSet<>();
    private List<String> rooms = new ArrayList<>(); // ✅ Fix: add rooms field

    public RoomResultPayload() {
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

    public List<String> getRooms() {
        return this.rooms;
    }

    public void setRooms(List<String> rooms) {
        this.rooms = rooms;
    }

    @Override
    public String toString() {
        return super.toString()
                + "\nWinner: " + this.winnerName
                + "\nChoices: " + this.playerChoices
                + "\nPoints: " + this.playerPoints
                + "\nEliminated: " + this.eliminatedPlayers
                + "\nRooms: " + this.rooms;
    }
}
