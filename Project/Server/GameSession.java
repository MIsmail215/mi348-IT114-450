// Updated: 2025-07-20 | UCID: Mi348
package Project.Server;

import java.util.*;
import java.util.concurrent.*;

import Project.Common.LoggerUtil;
import Project.Common.PayloadType;
import Project.Common.PointsPayload;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;


public class GameSession {
    private Room room;
    private Map<Long, PlayerState> players = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> roundTimer;
    private boolean inProgress = false;
    private int round = 0;

    public GameSession(Room room) {
        this.room = room;
    }

    // Starts the game session
    private void startSession() {
        inProgress = true;
        round = 0;
        LoggerUtil.INSTANCE.info(TextFX.colorize("GameSession: Starting game", Color.GREEN));
        for (ServerThread client : room.getClients()) {
            PlayerState p = new PlayerState(client);
            players.put(client.getClientId(), p);
        }
        startRound();
    }

    // Begins a new round
    private void startRound() {
        round++;
        LoggerUtil.INSTANCE.info(TextFX.colorize("GameSession: Starting Round " + round, Color.YELLOW));
        players.values().forEach(p -> {
            if (!p.isEliminated()) {
                p.setPick(null);
            }
        });

        broadcast("Starting Round " + round + ". Make your pick: /pick <r|p|s>");
        startRoundTimer();
    }

    // Ends the round either from all picks or timer
    private void endRound() {
        stopRoundTimer();
        LoggerUtil.INSTANCE.info(TextFX.colorize("GameSession: Ending Round " + round, Color.RED));

        List<PlayerState> active = getActivePlayers();
        List<PlayerState> missingPick = new ArrayList<>();
        for (PlayerState p : active) {
            if (p.getPick() == null) {
                p.setEliminated(true);
                missingPick.add(p);
            }
        }

        for (PlayerState p : missingPick) {
            broadcast(p.getName() + " was eliminated (no pick)");
        }

        resolveBattles();
        evaluateGameStatus();
    }

    // Called by Room when player sends /gameReady
    public synchronized void markReady(ServerThread sender, Collection<ServerThread> allPlayers) {
        if (inProgress) return;
        PlayerState p = players.getOrDefault(sender.getClientId(), new PlayerState(sender));
        p.setReady(true);
        players.put(sender.getClientId(), p);

        long readyCount = players.values().stream().filter(PlayerState::isReady).count();
        long total = allPlayers.size();

        broadcast(sender.getName() + " is ready (" + readyCount + "/" + total + ")");

        if (readyCount == total) {
            startSession();
        }
    }

    // Called by Room when player sends /pick <option>
    public synchronized void registerPick(ServerThread sender, String rawPick) {
        if (!inProgress) {
            sender.sendMessage(sender.getClientId(), "Game has not started. Type /ready first.");
            return;
        }

        PlayerState p = players.get(sender.getClientId());
        if (p == null || p.isEliminated()) return;

        String pick = rawPick.trim().toLowerCase();
        if (!Set.of("r", "p", "s").contains(pick)) {
            sender.sendMessage(sender.getClientId(), "Invalid pick. Use: r, p, or s.");
            return;
        }

        if (p.getPick() != null) {
            sender.sendMessage(sender.getClientId(), "You've already picked.");
            return;
        }

        p.setPick(pick);
        broadcast(p.getName() + " has locked in their pick.");

        if (allActivePicked()) {
            endRound();
        }
    }

    // Logic to resolve battles
    private void resolveBattles() {
        List<PlayerState> active = getActivePlayers();
        if (active.size() < 2) return;

        Map<PlayerState, Integer> wins = new HashMap<>();
        for (int i = 0; i < active.size(); i++) {
            PlayerState p1 = active.get(i);
            PlayerState p2 = active.get((i + 1) % active.size());

            String choice1 = p1.getPick();
            String choice2 = p2.getPick();

            int result = compare(choice1, choice2);
            String battleResult = p1.getName() + "(" + choice1 + ") vs " + p2.getName() + "(" + choice2 + ")";

            if (result == 1) {
                wins.put(p1, wins.getOrDefault(p1, 0) + 1);
                broadcast(battleResult + " -> " + p1.getName() + " wins");
            } else if (result == -1) {
                wins.put(p2, wins.getOrDefault(p2, 0) + 1);
                broadcast(battleResult + " -> " + p2.getName() + " wins");
            } else {
                broadcast(battleResult + " -> Tie");
            }
        }

        for (Map.Entry<PlayerState, Integer> entry : wins.entrySet()) {
            entry.getKey().addPoint();
            syncPoints(entry.getKey());
        }
    }

    // Checks game end condition
    private void evaluateGameStatus() {
        List<PlayerState> active = getActivePlayers();

        if (active.size() == 1) {
            broadcast(active.get(0).getName() + " is the winner!");
            endSession();
        } else if (active.isEmpty()) {
            broadcast("Game ended in a tie. No players remain.");
            endSession();
        } else {
            startRound();
        }
    }

    // Ends the session and resets all players
    private void endSession() {
        inProgress = false;
        stopRoundTimer();

        List<PlayerState> sorted = new ArrayList<>(players.values());
        sorted.sort(Comparator.comparing(PlayerState::getPoints).reversed());

        broadcast("== FINAL SCORES ==");
        for (PlayerState p : sorted) {
            broadcast(p.getName() + " - " + p.getPoints() + " point(s)");
        }

        players.clear();
    }

    private void broadcast(String msg) {
        room.relay(null, msg);
    }

    private void syncPoints(PlayerState p) {
        PointsPayload pp = new PointsPayload();
        pp.setClientId(p.getId());
        pp.setPoints(p.getPoints());
        pp.setPayloadType(PayloadType.SYNC_POINTS);
        room.broadcastPayload(pp);
    }

    private void startRoundTimer() {
        roundTimer = scheduler.schedule(() -> {
            LoggerUtil.INSTANCE.info("GameSession: Timer expired");
            endRound();
        }, 30, TimeUnit.SECONDS); // 30 second timer
    }

    private void stopRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel(false);
        }
    }

    private boolean allActivePicked() {
        return getActivePlayers().stream().allMatch(p -> p.getPick() != null);
    }

    private List<PlayerState> getActivePlayers() {
        List<PlayerState> result = new ArrayList<>();
        for (PlayerState p : players.values()) {
            if (!p.isEliminated()) result.add(p);
        }
        return result;
    }

    // Compares two picks
    private int compare(String a, String b) {
        if (a.equals(b)) return 0;
        if ((a.equals("r") && b.equals("s")) || (a.equals("p") && b.equals("r")) || (a.equals("s") && b.equals("p"))) {
            return 1;
        }
        return -1;
    }
}
