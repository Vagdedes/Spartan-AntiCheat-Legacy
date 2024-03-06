package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternValue {

    public final Number number;
    final PlayerProfile profile;
    public final long time;

    PatternValue(PlayerProfile profile, Number number, PatternGeneralization generalization, long time) {
        this.time = time;
        this.profile = profile;
        this.number = generalization.generalization == 0
                ? number
                : AlgebraUtils.cut(number.doubleValue(), generalization.generalization);
    }
}
