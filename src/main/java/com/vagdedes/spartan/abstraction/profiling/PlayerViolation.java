package com.vagdedes.spartan.abstraction.profiling;

import me.vagdedes.spartan.system.Enums;

public class PlayerViolation {

    public final Enums.HackType hackType;
    public final long time;
    public final String information;
    public final int level, increase;

    public PlayerViolation(long time,
                           Enums.HackType hackType,
                           String information,
                           int level,
                           int increase) {
        this.hackType = hackType;
        this.time = time;
        this.information = information;
        this.level = level;
        this.increase = increase;
    }

    public int sum() {
        return this.level + this.increase;
    }

}
