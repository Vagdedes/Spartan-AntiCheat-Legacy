package com.vagdedes.spartan.objects.profiling;

import com.vagdedes.spartan.handlers.tracking.CombatProcessing;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class PlayerOpponent {

    private final String name;
    private final SpartanPlayer player;
    private int hits, damage, maxCPS, hitCombo, maxHitCombo;
    private long lastHit, previousLastHit, lastDamage, hitTimeAverage, duration;
    private double reachAverage, yawRateAverage, pitchRateAverage, cpsAverage;
    private final long start;

    // Separator

    PlayerOpponent(SpartanPlayer player) { // Used for memory
        long time = System.currentTimeMillis();

        this.name = player.getName();
        this.player = player;
        this.hits = 0;
        this.damage = 0;
        this.maxCPS = 1;
        this.maxHitCombo = 0;

        this.cpsAverage = 0;
        this.reachAverage = 0;
        this.hitTimeAverage = 0;
        this.yawRateAverage = 0;
        this.pitchRateAverage = 0;

        this.hitCombo = 0;
        this.lastDamage = 0L;
        this.lastHit = time;
        this.previousLastHit = time;
        this.start = time;
        this.duration = 0L;
    }

    // Separator

    public String getName() {
        return name;
    }

    public SpartanPlayer getPlayer() {
        return player;
    }

    public int getHits() {
        return hits;
    }

    public int getMaxCPS() {
        return maxCPS;
    }

    public long getDuration() {
        return duration;
    }

    public int getMaxHitCombo() {
        return maxHitCombo;
    }

    public boolean hasHitCombo(int number) {
        return hitCombo >= number;
    }

    public boolean hasHitCombo() {
        return hasHitCombo(2);
    }

    public int getCurrentHitCombo() {
        return hitCombo;
    }

    // Separator

    public double getHitsAverage() {
        return hits / (double) duration;
    }

    public long getHitTimeAverage() {
        return hitTimeAverage / hits;
    }

    public double getReachAverage() {
        return reachAverage / (double) hits;
    }

    public double getCPSAverage() {
        return cpsAverage / (double) (hits + damage);
    }

    public double getYawRateAverage() {
        return yawRateAverage / (double) (hits + damage);
    }

    public double getPitchRateAverage() {
        return pitchRateAverage / (double) (hits + damage);
    }

    // Separator

    private void update(SpartanPlayer p, boolean lastDamage) {
        int cps = p.getClicks().getCount();

        if (cps > this.maxCPS) {
            this.maxCPS = cps;
        }
        this.cpsAverage += cps;
        this.yawRateAverage += CombatProcessing.getDecimal(p, CombatProcessing.yawDifference, 0.0);
        this.pitchRateAverage += CombatProcessing.getDecimal(p, CombatProcessing.pitchDifference, 0.0);
        this.maxHitCombo = Math.max(maxHitCombo, hitCombo);

        if (lastDamage) {
            this.damage++;
            this.lastDamage = System.currentTimeMillis();
            this.hitCombo = 0;
        }
    }

    public void update() {
        update(player, true);
    }

    public void damager(double distance) {
        if (!hasEnded()) { // Asynchronous Protection
            long lastHit = getLastHit();
            this.previousLastHit = lastHit;
            this.lastHit = System.currentTimeMillis();

            this.hits++;
            this.hitCombo++;

            this.reachAverage += distance;
            this.hitTimeAverage += lastHit;
            update(player, false);
        }
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

    boolean hasEnded() {
        return getLastHit() > PlayerFight.maxFightTime || duration != 0L;
    }

    // Separator

    void end(long time) {
        this.duration = time - start;
    }

}
