package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternValue {
    
    public final Number pattern;
    final long situation;
    final long time;

    PatternValue(long situation, Number number, PatternGeneralization patternGeneralization, long time) {
        this.time = time;
        this.pattern = AlgebraUtils.generalize(number, patternGeneralization.initiator.generalization);
        this.situation = situation;
    }
}
