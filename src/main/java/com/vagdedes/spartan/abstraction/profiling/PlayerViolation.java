package com.vagdedes.spartan.abstraction.profiling;

public class PlayerViolation {

    public final long time;
    public final int hash;
    public final double increase;

    public PlayerViolation(long time,
                           double increase,
                           String detection) {
        this.time = time;
        this.increase = increase;
        this.hash = detection.hashCode();
    }

}
