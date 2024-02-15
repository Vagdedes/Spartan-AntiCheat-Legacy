package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;

public class PlayerFight {

    // Properties
    public static final long maxFightTime = 20_000L;
    public static final int requiredHits = 4;

    // Separator

    private final PlayerOpponent opponent1, opponent2;
    private PlayerOpponent winner, loser;
    private boolean judged;

    PlayerFight(PlayerOpponent opponent1, PlayerOpponent opponent2) { // Used for memory
        this.opponent1 = opponent1;
        this.opponent2 = opponent2;
        this.winner = null;
        this.loser = null;
        this.judged = false;
        opponent1.getPlayer().getProfile().getCombat().createFight(this);
        opponent2.getPlayer().getProfile().getCombat().createFight(this);
    }

    // Separator

    public PlayerOpponent[] getOpponent(PlayerOpponent opponent) { // Use only when player is in said fight
        PlayerOpponent opponent1 = getOpponent1();
        return opponent1 == opponent ?
                new PlayerOpponent[]{opponent1, getOpponent2()} :
                new PlayerOpponent[]{getOpponent2(), opponent1};
    }

    public PlayerOpponent[] getOpponent(SpartanPlayer player) {
        return opponent1.getPlayer().equals(player) ?
                new PlayerOpponent[]{opponent1, getOpponent2()} :
                new PlayerOpponent[]{getOpponent2(), opponent1};
    }

    // Separator

    public PlayerOpponent getOpponent1() {
        return opponent1;
    }

    public PlayerOpponent getOpponent2() {
        return opponent2;
    }

    // Separator

    public double getHitRatio(PlayerOpponent[] opponents) {
        int targetHits = opponents[1].getHits();
        return targetHits == 0 ? opponents[0].getHits() : (opponents[0].getHits() / ((double) targetHits));
    }

    public double getHitRatio(PlayerOpponent opponent) {
        return getHitRatio(getOpponent(opponent));
    }

    public int getHits() {
        return opponent1.getHits() + opponent2.getHits();
    }

    public boolean isJudged() {
        return judged;
    }

    public boolean isKill() {
        return !judged;
    }

    public long getDuration() {
        return Math.min(opponent1.getDuration(), opponent2.getDuration());
    }

    // Separator

    public void setWinner(SpartanPlayer opponent) { // Use only when player is in said fight
        long time = System.currentTimeMillis();
        PlayerOpponent[] opponents = getOpponent(opponent);
        PlayerOpponent winnerOpponent = opponents[0];
        PlayerOpponent loserOpponent = opponents[1];
        winnerOpponent.end(time);
        loserOpponent.end(time);
        this.winner = winnerOpponent;
        this.loser = loserOpponent;
    }

    public void track() {
        PlayerOpponent opponent1 = getOpponent1();
        PlayerOpponent opponent2 = getOpponent2();
        int opponentHits1 = opponent1.getHits();
        int opponentHits2 = opponent2.getHits();

        long time = System.currentTimeMillis();
        opponent1.end(time);
        opponent2.end(time);
        this.judged = true;

        if (opponentHits1 > opponentHits2) {
            this.winner = opponent1;
            this.loser = opponent2;
            store();
        } else if (opponentHits1 != opponentHits2) {
            this.winner = opponent2;
            this.loser = opponent1;
            store();
        } else { // No draw outcome
            delete();
        }
    }

    // Separator

    private void delete() {
        for (PlayerOpponent opponent : new PlayerOpponent[]{opponent1, opponent2}) {
            opponent.getPlayer().getProfile().getCombat().removeFight(this);
        }
    }

    private void store() {
        // Memory
        for (PlayerOpponent opponent : new PlayerOpponent[]{opponent1, opponent2}) {
            opponent.getPlayer().getProfile().getCombat().removeFight(this);
        }

        // Detections
        SpartanPlayer player = SpartanBukkit.getPlayer(winner.getName());

        if (player != null) {
            player.getExecutor(Enums.HackType.KillAura).handle(false, winner);
        }
    }
}
