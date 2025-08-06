package Project.Common;

public enum PlayerStatus {
    ACTIVE,       // In the game, waiting for the round to start
    WAITING,      // Round has started, waiting for this player to pick
    PICKED,       // Player has locked in their pick for the round
    AWAY,         // Player is in the room but sitting out of the game
    SPECTATING,   // User is watching the game, not playing
    ELIMINATED    // Player has been eliminated from the game
}