package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternValue {

    public final Number pattern;
    final long situation;
    final long time;

    PatternValue(long situation, Number number, int generalization, long time) {
        this.time = time;
        this.pattern = generalization == 0
                ? number
                : AlgebraUtils.cut(number.doubleValue(), generalization);
        this.situation = situation;
    }
}
