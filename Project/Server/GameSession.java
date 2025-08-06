// UCID: Mi348
// Date: 2025-08-06
package Project.Server;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import Project.Common.*;
import Project.Common.TextFX.Color;

public class GameSession {
    private Room room;
    private Map<Long, PlayerState> players = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> roundTimer;
    private boolean inProgress = false;
    private int round = 0;
    private static final int TOTAL_ROUNDS = 5;
    private static final int ROUND_TIME_SECONDS = 30;

    private boolean extraOptionsEnabled = false;
    private boolean cooldownEnabled = false;

    public GameSession(Room room) {
        this.room = room;
    }
    
    // vvv THIS IS THE CORRECTED METHOD vvv
    public synchronized void markReady(ServerThread sender, Payload readyPayload) {
        if (inProgress || sender.isSpectator()) return;

        // The first non-spectator to ready up is the host and sets the rules
        if (getGamePlayers().stream().noneMatch(PlayerState::isReady)) {
            if (readyPayload instanceof ReadyPayload rp) {
                this.extraOptionsEnabled = rp.areExtraOptionsEnabled();
                this.cooldownEnabled = rp.isCooldownEnabled();
                String settings = String.format("Game settings set by host: Extra Options [%b], Cooldown [%b]", extraOptionsEnabled, cooldownEnabled);
                broadcast(settings);
            }
        }

        PlayerState p = players.computeIfAbsent(sender.getClientId(), id -> new PlayerState(sender));
        
        // Prevent a 4th player from readying up if 3 are already ready
        if (getGamePlayers().stream().filter(PlayerState::isReady).count() >= 3 && !p.isReady()) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "The game is full with 3 players. Please wait for the next match.");
            return;
        }

        p.setReady(true);
        
        long readyCount = getGamePlayers().stream().filter(PlayerState::isReady).count();
        
        broadcast(sender.getClientName() + " is ready (" + readyCount + "/3)");
        
        // Start the game only when there are exactly 3 ready players
        if (readyCount == 3) {
            startSession();
        }
    }
    // ^^^ END OF CORRECTION ^^^

    private void evaluateGameStatus() {
        if (round >= TOTAL_ROUNDS) {
            broadcast("5 rounds are complete! The game is over!");
            endSession();
        } else {
            startRound();
        }
    }

    private void resolveBattles() {
        List<PlayerState> active = getActivePlayers();
        if (active.size() < 2) return;

        broadcast("--- Round " + round + " Results ---");
        
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                PlayerState p1 = active.get(i);
                PlayerState p2 = active.get(j);
                String choice1 = p1.getPick();
                String choice2 = p2.getPick();
                int result = compare(choice1, choice2);
                String battleLog = String.format("%s (%s) vs %s (%s)", p1.getName(), choice1, p2.getName(), choice2);

                if (result == 1) {
                    p1.addPoint();
                    syncPoints(p1);
                    broadcast(battleLog + " -> " + p1.getName() + " wins!");
                } else if (result == -1) {
                    p2.addPoint();
                    syncPoints(p2);
                    broadcast(battleLog + " -> " + p2.getName() + " wins!");
                } else {
                    broadcast(battleLog + " -> Tie!");
                }
            }
        }
    }
    
    private void endSession() {
        inProgress = false;
        stopRoundTimer();

        int maxScore = -1;
        for (PlayerState p : getGamePlayers()) {
            if (p.getPoints() > maxScore) {
                maxScore = p.getPoints();
            }
        }

        final int finalMaxScore = maxScore;
        List<String> winners = getGamePlayers().stream()
                .filter(p -> p.getPoints() == finalMaxScore && finalMaxScore > 0)
                .map(PlayerState::getName)
                .collect(Collectors.toList());

        if (winners.size() == 1) {
            broadcast(winners.get(0) + " is the winner with " + maxScore + " points!");
        } else if (winners.size() > 1) {
            broadcast("It's a tie between: " + String.join(", ", winners) + " with " + maxScore + " points!");
        } else {
            broadcast("Game over! No one scored any points.");
        }

        GameResultPayload finalResult = new GameResultPayload();
        if (!winners.isEmpty()) {
            finalResult.setWinnerName(String.join(", ", winners));
        }
        Map<User, Integer> finalPoints = new HashMap<>();
        for (PlayerState ps : getGamePlayers()) {
            finalPoints.put(ps.getUser(), ps.getPoints());
        }
        finalResult.setPlayerPoints(finalPoints);
        room.broadcastPayload(finalResult);

        broadcast("== GAME OVER ==");
        
        Payload resetPayload = new Payload();
        resetPayload.setPayloadType(PayloadType.RESET_GAME_STATE);
        room.broadcastPayload(resetPayload);

        players.clear();
    }
    
    // --- (The rest of the file is unchanged, but is included for completeness) ---
    public synchronized void toggleAwayStatus(ServerThread sender) {
        if(sender.isSpectator()) return;
        PlayerState p = players.computeIfAbsent(sender.getClientId(), id -> new PlayerState(sender));
        p.setAway(!p.isAway());
        if (p.isAway()) {
            p.setStatus(PlayerStatus.AWAY);
            broadcast(p.getName() + " is now away.");
        } else {
            p.setStatus(PlayerStatus.ACTIVE);
            broadcast(p.getName() + " is no longer away.");
        }
        syncPlayerStatus(p);
    }
    private List<PlayerState> getGamePlayers() {
        return players.values().stream()
                .filter(p -> !p.isSpectator())
                .collect(Collectors.toList());
    }
    private List<PlayerState> getActivePlayers() {
        return getGamePlayers().stream()
                .filter(p -> !p.isEliminated() && !p.isAway())
                .collect(Collectors.toList());
    }
    private void startSession() {
        inProgress = true;
        round = 0;
        LoggerUtil.INSTANCE.info(TextFX.colorize("GameSession: Starting game", TextFX.Color.GREEN));
        
        getGamePlayers().forEach(p -> {
            p.setEliminated(false);
            p.setPoints(0);
            p.setReady(false);
            p.setStatus(PlayerStatus.ACTIVE);
            syncPlayerStatus(p);
        });
        startRound();
    }
    private void startRound() {
        round++;
        LoggerUtil.INSTANCE.info(TextFX.colorize("GameSession: Starting Round " + round, TextFX.Color.YELLOW));
        getActivePlayers().forEach(p -> {
            p.setPick(null);
            p.setStatus(PlayerStatus.WAITING);
            syncPlayerStatus(p);
        });
        RoundStartPayload roundStartPayload = new RoundStartPayload(round, ROUND_TIME_SECONDS);
        room.broadcastPayload(roundStartPayload);
        startRoundTimer();
    }
    private void endRound() {
        stopRoundTimer();
        LoggerUtil.INSTANCE.info(TextFX.colorize("GameSession: Ending Round " + round, TextFX.Color.RED));
        List<PlayerState> active = getActivePlayers();
        for (PlayerState p : active) {
            if (p.getPick() == null) {
                broadcast(p.getName() + " did not make a pick.");
            }
        }
        resolveBattles();
        evaluateGameStatus();
    }
    public synchronized void registerPick(ServerThread sender, String rawPick) {
        if (!inProgress) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Game has not started.");
            return;
        }
        PlayerState p = players.get(sender.getClientId());
        if (p == null || p.isEliminated() || p.isAway() || p.isSpectator()) return;
        String pick = rawPick.trim().toLowerCase();
        
        Set<String> validPicks = new HashSet<>(Set.of("rock", "paper", "scissors"));
        if (extraOptionsEnabled) {
            validPicks.add("lizard");
            validPicks.add("spock");
        }
        
        if (!validPicks.contains(pick)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid pick for the current game rules.");
            return;
        }

        if (p.getPick() != null) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "You've already picked for this round.");
            return;
        }
        p.setPick(pick);
        p.setStatus(PlayerStatus.PICKED);
        syncPlayerStatus(p);
        broadcast(p.getName() + " has locked in their pick.");
        if (allActivePicked()) {
            endRound();
        }
    }
    private void syncPlayerStatus(PlayerState player) {
        PlayerStatusPayload psp = new PlayerStatusPayload(player.getId(), player.getStatus());
        room.broadcastPayload(psp);
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
        }, ROUND_TIME_SECONDS, TimeUnit.SECONDS);
    }
    private void stopRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel(false);
        }
    }
    private boolean allActivePicked() {
        return getActivePlayers().stream().allMatch(p -> p.getPick() != null);
    }
    private int compare(String a, String b) {
        if (a == null || b == null || a.equals(b)) return 0;
        return switch (a) {
            case "rock" -> (b.equals("scissors") || b.equals("lizard")) ? 1 : -1;
            case "paper" -> (b.equals("rock") || b.equals("spock")) ? 1 : -1;
            case "scissors" -> (b.equals("paper") || b.equals("lizard")) ? 1 : -1;
            case "lizard" -> (b.equals("spock") || b.equals("paper")) ? 1 : -1;
            case "spock" -> (b.equals("scissors") || b.equals("rock")) ? 1 : -1;
            default -> 0;
        };
    }
}