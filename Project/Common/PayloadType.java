// Updated for Milestone 2 on 07/20/2025 by Mi348

package Project.Common;

public enum PayloadType {
    CLIENT_CONNECT,     // client requesting to connect to server (passing name)
    CLIENT_ID,          // server assigning a client id
    SYNC_CLIENT,        // used for syncing clients in a room
    DISCONNECT,         // client wants to disconnect

    ROOM_CREATE,        // create a new room
    ROOM_JOIN,          // join an existing room
    ROOM_REMOVE,        // remove/delete a room
    ROOM_LEAVE,         // leave a room
    ROOM_LIST,          // list all rooms

    REVERSE,            // send a reversed string back
    MESSAGE,            // broadcast message to room

    // GAME session-specific payloads (added for Milestone 2)
    GAME_READY,         // player is ready to start
    GAME_PICK,          // player has made a choice (e.g., rock, paper)
    GAME_RESULT,        // result of a round
    GAME_STATE,         // current game state (waiting, round, eliminated)
    ROUND_START,        // signal start of a round
    ROUND_END,          // signal end of a round
    PLAYER_ELIMINATED,  // player has been eliminated
    SCOREBOARD,         // current score standings
    SESSION_START, 
    SYNC_POINTS,     // game session has started
    SESSION_END         // game session has ended
}
