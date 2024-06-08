package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

public class KeepSprint {

    private int hits;
    private boolean sprinted;
    private int flags;
    private long timedOut;

    KeepSprint() {
        this(0, false, 0, System.currentTimeMillis());
    }

    KeepSprint(int hits, boolean sprinted, int flags, long timedOut) {
        this.hits = hits;
        this.sprinted = sprinted;
        this.flags = flags;
        this.timedOut = timedOut;
    }

    int hits() {
        return hits;
    }

    boolean sprinted() {
        return sprinted;
    }

    int flags() {
        return flags;
    }

    long timedOut() {
        return timedOut;
    }

    void newHit() {
        this.hits++;
    }

    void newFlag() {
        this.flags++;
    }

    void setTimedOut(long time) {
        this.timedOut = time;
    }

    boolean isTimedOut() {
        return (System.currentTimeMillis() - this.timedOut) > 350;
    }

    void sprint() {
        this.sprinted = true;
    }
}
