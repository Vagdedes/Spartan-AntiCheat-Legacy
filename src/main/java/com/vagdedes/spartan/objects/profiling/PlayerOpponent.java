package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class PlayerOpponent {

    private final SpartanPlayer player;
    private int hits, damage, hitCombo;
    private long lastHit, lastDamage;

    // Separator

    PlayerOpponent(SpartanPlayer player) { // Used for memory
        long time = System.currentTimeMillis();

        this.player = player;
        this.hits = 0;
        this.damage = 0;

        this.hitCombo = 0;
        this.lastDamage = 0L;
        this.lastHit = time;
    }

    // Separator

    public SpartanPlayer getPlayer() {
        return player;
    }

    public int getHits() {
        return hits;
    }

    public int getDamage() {
        return damage;
    }

    public boolean hasHitCombo() {
        return hitCombo >= 2;
    }

    public int getHitCombo() {
        return hitCombo;
    }

    // Separator

    public void update() {
        this.damage++;
        this.lastDamage = System.currentTimeMillis();
        this.hitCombo = 0;
    }

    public void damager() {
        this.lastHit = System.currentTimeMillis();

        this.hits++;
        this.hitCombo++;
    }

    // Separator

    public long getLastHit(boolean maxIfNull) {
        return this.lastHit == 0L ? (maxIfNull ? Long.MAX_VALUE : 0L) : System.currentTimeMillis() - this.lastHit;
    }

    public long getLastHit() {
        return getLastHit(true);
    }

    public long getLastDamage() {
        return System.currentTimeMillis() - this.lastDamage;
    }

}
