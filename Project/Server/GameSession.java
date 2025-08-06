package Project.Server;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import Project.Common.*;

public class GameSession {
    private Room room;
    private Map<Long, PlayerState> players = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> roundTimer;
    private boolean inProgress = false;
    private int round = 0;
    private static final int ROUND_TIME_SECONDS = 30;
    private boolean extraOptionsEnabled = false;
    private boolean cooldownEnabled = false;

    public GameSession(Room room) {
        this.room = room;
    }

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
                p.setEliminated(true);
                syncPlayerStatus(p);
                broadcast(p.getName() + " was eliminated for not making a pick.");
            }
        }
        resolveBattles();
        evaluateGameStatus();
    }
    public synchronized void markReady(ServerThread sender, Payload readyPayload) {
        if (inProgress || sender.isSpectator()) return;
        
        if (getGamePlayers().isEmpty() || sender.getClientId() == getGamePlayers().get(0).getId()) {
            if (readyPayload instanceof ReadyPayload rp) {
                this.extraOptionsEnabled = rp.areExtraOptionsEnabled();
                this.cooldownEnabled = rp.isCooldownEnabled();
                String settings = String.format("Game settings set by host: Extra Options [%b], Cooldown [%b]", extraOptionsEnabled, cooldownEnabled);
                broadcast(settings);
            }
        }

        PlayerState p = players.computeIfAbsent(sender.getClientId(), id -> new PlayerState(sender));
        p.setReady(true);
        
        long readyCount = getGamePlayers().stream().filter(PlayerState::isReady).count();
        long totalPlayersInGame = getGamePlayers().size();

        broadcast(sender.getClientName() + " is ready (" + readyCount + "/" + totalPlayersInGame + ")");
        if (readyCount >= 2 && readyCount == totalPlayersInGame) {
            startSession();
        }
    }
    public synchronized void registerPick(ServerThread sender, String rawPick) {
        if (!inProgress) {
            sender.sendMessage(sender.getClientId(), "Game has not started.");
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
            sender.sendMessage(sender.getClientId(), "Invalid pick for the current game rules.");
            return;
        }

        if (p.getPick() != null) {
            sender.sendMessage(sender.getClientId(), "You've already picked for this round.");
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
    private void resolveBattles() {
        List<PlayerState> active = getActivePlayers();
        if (active.size() < 2) return;
        Set<String> uniquePicks = active.stream().map(PlayerState::getPick).collect(Collectors.toSet());
        if (uniquePicks.size() != 2) {
            broadcast("Round is a draw! No players eliminated.");
            awardPointsForIndividualWins(active);
            return;
        }
        List<String> picks = new ArrayList<>(uniquePicks);
        String pickA = picks.get(0);
        String pickB = picks.get(1);
        int result = compare(pickA, pickB);
        String winningPick = (result == 1) ? pickA : pickB;
        String losingPick = (result == 1) ? pickB : pickA;
        broadcast(winningPick.substring(0, 1).toUpperCase() + winningPick.substring(1) + " beats " + losingPick + "!");
        for (PlayerState player : active) {
            if (player.getPick().equals(winningPick)) {
                player.addPoint();
                syncPoints(player);
                broadcast(player.getName() + " wins the round!");
            } else {
                player.setEliminated(true);
                syncPlayerStatus(player);
                broadcast(player.getName() + " was eliminated!");
            }
        }
    }
    private void awardPointsForIndividualWins(List<PlayerState> active) {
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                PlayerState p1 = active.get(i);
                PlayerState p2 = active.get(j);
                int result = compare(p1.getPick(), p2.getPick());
                if(result == 1) {
                    p1.addPoint();
                    syncPoints(p1);
                    broadcast(p1.getName() + " won a battle against " + p2.getName());
                } else if(result == -1) {
                    p2.addPoint();
                    syncPoints(p2);
                    broadcast(p2.getName() + " won a battle against " + p1.getName());
                }
            }
        }
    }
    private void evaluateGameStatus() {
        List<PlayerState> active = getActivePlayers();
        if (active.size() <= 1) {
            if (active.size() == 1) {
                broadcast(active.get(0).getName() + " is the winner!");
            } else {
                broadcast("Game ended in a draw! No players remain.");
            }
            endSession();
        } else {
            startRound();
        }
    }
    private void endSession() {
        inProgress = false;
        stopRoundTimer();
        GameResultPayload finalResult = new GameResultPayload();
        List<PlayerState> sortedPlayers = getGamePlayers().stream()
            .sorted(Comparator.comparing(PlayerState::getPoints).reversed())
            .collect(Collectors.toList());
        if (!sortedPlayers.isEmpty() && getActivePlayers().size() == 1) {
            finalResult.setWinnerName(getActivePlayers().get(0).getName());
        }
        Map<User, Integer> finalPoints = new HashMap<>();
        for (PlayerState ps : sortedPlayers) {
            finalPoints.put(ps.getUser(), ps.getPoints());
        }
        finalResult.setPlayerPoints(finalPoints);
        room.broadcastPayload(finalResult);
        broadcast("== GAME OVER ==");
        players.values().forEach(p -> p.setReady(false));
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