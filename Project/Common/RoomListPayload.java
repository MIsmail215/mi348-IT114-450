// UCID: mi348
// Date: 2025-08-05
package Project.Common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RoomListPayload extends Payload {
    private List<String> rooms;

    // Game result fields (added to support Client.java UI)
    private String winnerName;
    private Map<User, String> playerChoices = new HashMap<>();
    private Map<User, Integer> playerPoints = new HashMap<>();
    private Set<User> eliminatedPlayers = new HashSet<>();

    public RoomListPayload() {
        setPayloadType(PayloadType.ROOM_LIST);
    }

    public List<String> getRooms() {
        return rooms;
    }

    public void setRooms(List<String> rooms) {
        this.rooms = rooms;
    }

    // Added getters/setters for game result data

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public Map<User, String> getPlayerChoices() {
        return playerChoices;
    }

    public void setPlayerChoices(Map<User, String> playerChoices) {
        this.playerChoices = playerChoices;
    }

    public Map<User, Integer> getPlayerPoints() {
        return playerPoints;
    }

    public void setPlayerPoints(Map<User, Integer> playerPoints) {
        this.playerPoints = playerPoints;
    }

    public Set<User> getEliminatedPlayers() {
        return eliminatedPlayers;
    }

    public void setEliminatedPlayers(Set<User> eliminatedPlayers) {
        this.eliminatedPlayers = eliminatedPlayers;
    }

    @Override
    public String toString() {
        return super.toString() +
               "\nRooms: " + rooms +
               "\nWinner: " + winnerName +
               "\nChoices: " + playerChoices +
               "\nPoints: " + playerPoints +
               "\nEliminated: " + eliminatedPlayers;
    }
}
