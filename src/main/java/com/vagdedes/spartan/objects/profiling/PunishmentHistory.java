package com.vagdedes.spartan.objects.profiling;

public class PunishmentHistory {

    private int kicks, warnings;

    PunishmentHistory() {
        this.kicks = 0;
        this.warnings = 0;
    }

    public int getOverall() {
        return kicks + warnings;
    }

    public int getKicks() {
        return kicks;
    }

    public void increaseKicks() {
        kicks += 1;
    }

    public int getWarnings() {
        return warnings;
    }

    public void increaseWarnings() {
        warnings += 1;
    }
}
