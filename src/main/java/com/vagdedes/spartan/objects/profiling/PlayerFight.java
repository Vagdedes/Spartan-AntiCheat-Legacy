package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;

public class PlayerFight {

    // Properties
    public static final long maxFightTime = 20_000L;
    public static final int requiredHits = 4;

    // Separator

    final PlayerOpponent opponent1, opponent2;
    private long start, end;
    private boolean judged;

    PlayerFight(PlayerOpponent opponent1, PlayerOpponent opponent2) { // Used for memory
        this.opponent1 = opponent1;
        this.opponent2 = opponent2;
        this.judged = false;
    }

    public PlayerOpponent[] getOpponent(SpartanPlayer player) {
        return opponent1.getPlayer().equals(player) ?
                new PlayerOpponent[]{opponent1, opponent2} :
                new PlayerOpponent[]{opponent2, opponent1};
    }

    // Separator

    public double getHitRatio(PlayerOpponent[] opponents) {
        int targetHits = opponents[1].getHits();
        return targetHits == 0 ? opponents[0].getHits() : (opponents[0].getHits() / ((double) targetHits));
    }

    public double getHitRatio(SpartanPlayer player) {
        return getHitRatio(getOpponent(player));
    }

    public int getHits() {
        return opponent1.getHits() + opponent2.getHits();
    }

    public boolean isKill() {
        return !judged;
    }

    public long getDuration() {
        return Math.min(opponent1.getDuration(), opponent2.getDuration());
    }

    // Separator

    public void setWinner(SpartanPlayer opponent) { // Use only when player is in said fight
        finish(getOpponent(opponent)[0]);
    }

    boolean expired() {
        this.judged = true;

        if (opponent1.getHits() > opponent2.getHits()) {
            finish(opponent1);
        } else if (opponent2.getHits() > opponent1.getHits()) {
            finish(opponent2);
        }
        return false;
    }

    private void finish(PlayerOpponent winner) {
        winner.getPlayer().getExecutor(Enums.HackType.KillAura).handle(false, this);
    }
}
