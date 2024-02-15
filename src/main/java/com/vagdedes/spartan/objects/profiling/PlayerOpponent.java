package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class PlayerOpponent {

    private final SpartanPlayer player;
    private int hits, damage, hitCombo;
    private long lastHit, previousLastHit, lastDamage;

    // Separator

    PlayerOpponent(SpartanPlayer player) { // Used for memory
        long time = System.currentTimeMillis();

        this.player = player;
        this.hits = 0;
        this.damage = 0;

        this.hitCombo = 0;
        this.lastDamage = 0L;
        this.lastHit = time;
        this.previousLastHit = time;
    }

    // Separator

    public SpartanPlayer getPlayer() {
        return player;
    }

    public int getHits() {
        return hits;
    }

    public boolean hasHitCombo(int number) {
        return hitCombo >= number;
    }

    public boolean hasHitCombo() {
        return hasHitCombo(2);
    }

    public int getHitCombo() {
        return hitCombo;
    }

    // Separator

    private void update(boolean lastDamage) {
        if (lastDamage) {
            this.damage++;
            this.lastDamage = System.currentTimeMillis();
            this.hitCombo = 0;
        }
    }

    public void update() {
        update(true);
    }

    public void damager() {
        this.previousLastHit = getLastHit();
        this.lastHit = System.currentTimeMillis();

        this.hits++;
        this.hitCombo++;
        update(false);
    }

    // Separator

    public long getLastHit(boolean maxIfNull) {
        return this.lastHit == 0L ? (maxIfNull ? Long.MAX_VALUE : 0L) : System.currentTimeMillis() - this.lastHit;
    }

    public long getLastHit() {
        return getLastHit(true);
    }

    public long getPreviousLastHit(boolean maxIfNull) { // Replacement to 'getLastHit' in situations where data is just updated
        return this.previousLastHit == 0L ? (maxIfNull ? Long.MAX_VALUE : 0L) : System.currentTimeMillis() - this.previousLastHit;
    }

    public long getPreviousLastHit() { // Replacement to 'getLastHit' in situations where data is just updated
        return getPreviousLastHit(true);
    }

    public long getLastDamage() {
        return System.currentTimeMillis() - this.lastDamage;
    }

}
