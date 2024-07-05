package com.vagdedes.spartan.abstraction.profiling;

import me.vagdedes.spartan.system.Enums;

public class PlayerViolation {

    public final Enums.HackType hackType;
    public final long time;
    public final String information, detection;
    public final int level, increase, identity;

    public PlayerViolation(long time,
                           Enums.HackType hackType,
                           String information,
                           int level,
                           int increase,
                           InformationAnalysis analysis) {
        this.hackType = hackType;
        this.time = time;
        this.information = information;
        this.level = level;
        this.increase = increase;

        this.detection = analysis.detection;
        this.identity = analysis.identity;

        if (this.increase > 1) {
            this.hackType.getCheck().enableMultiViolations();
        }
    }

    public PlayerViolation(long time,
                           Enums.HackType hackType,
                           String information,
                           int level,
                           int increase) {
        this(
                time,
                hackType,
                information,
                level,
                increase,
                new InformationAnalysis(hackType, information)
        );
    }

    public int getIgnoredViolations(Enums.DataType dataType) {
        return hackType.getCheck().getIgnoredViolations(dataType, this.identity);
    }

}
