package com.vagdedes.spartan.abstraction.pattern.implementation.base;

public class PatternValue {

    public final Number pattern;
    final long situation;
    final long time;

    PatternValue(long situation, Number number, PatternGeneralization patternGeneralization, long time) {
        this.time = time;
        this.pattern = PatternGeneralization.generalize(number, patternGeneralization.generalization);
        this.situation = situation;
    }
}
