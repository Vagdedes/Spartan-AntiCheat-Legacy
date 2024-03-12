package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternValue {

    public final Number number;
    final long time;

    PatternValue(Number number, PatternGeneralization generalization, long time) {
        this.time = time;
        this.number = generalization.generalization == 0
                ? number
                : AlgebraUtils.cut(number.doubleValue(), generalization.generalization);
    }
}
