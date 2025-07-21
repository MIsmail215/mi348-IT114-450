// Updated: 2025-07-20 | UCID: Mi348

package Project.Common;

import java.util.HashMap;

public enum Command {
    // Session control commands
    QUIT("quit"),
    DISCONNECT("disconnect"),
    LOGOUT("logout"),
    LOGOFF("logoff"),

    // Utility and room management commands
    REVERSE("reverse"),
    CREATE_ROOM("createroom"),
    LEAVE_ROOM("leaveroom"),
    JOIN_ROOM("joinroom"),
    NAME("name"),
    LIST_USERS("users"),
    LIST_ROOMS("listrooms"),

    // Milestone 2 game session commands
    READY("ready"),   // Used to signal that a player is ready
    PICK("pick");     // Used to pick a move like rock, paper, scissors

    // Map to link command strings to enum
    private static final HashMap<String, Command> BY_COMMAND = new HashMap<>();

    static {
        for (Command e : values()) {
            BY_COMMAND.put(e.command, e);
        }
    }

    // The actual command string (e.g., "ready", "pick")
    public final String command;

    private Command(String command) {
        this.command = command;
    }

    // Lookup method for string to Command enum
    public static Command stringToCommand(String command) {
        return BY_COMMAND.get(command);
    }
}
