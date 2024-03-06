package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.objects.profiling.PlayerProfile;

public class PatternValue {

    public final Number number;
    public final PlayerProfile profile;
    public final long time;

    PatternValue(PlayerProfile profile, Number number) {
        this(profile, number, System.currentTimeMillis());
    }

    PatternValue(PlayerProfile profile, Number number, long time) {
        this.profile = profile;
        this.number = number;
        this.time = time;
    }
}
