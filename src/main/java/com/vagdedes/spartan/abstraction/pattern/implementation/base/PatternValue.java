package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternValue {

    public final Number pattern;
    final long situation;
    final long time;

    PatternValue(long situation, Number number, int generalization, long time) {
        this.time = time;
        this.pattern = PatternGeneralization.generalize(number, generalization);
        this.situation = situation;
    }
}
