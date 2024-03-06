package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternValue {

    public final Number number;
    final PlayerProfile profile;
    final long time;

    // Use for cache to be stored
    PatternValue(PlayerProfile profile, Number number, PatternGeneralization generalization, long time) {
        this.time = time;
        this.profile = profile;
        this.number = generalization.generalization == 0
                ? number
                : AlgebraUtils.cut(number.doubleValue(), generalization.generalization);
    }

    // Use for cache that's already stored
    PatternValue(PlayerProfile profile, Number number, long time) {
        this.time = time;
        this.profile = profile;
        this.number = number;
    }
}
