package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternGeneralizationInitiator {

    final short
            scissors,
            line,
            square,
            cube;

    public PatternGeneralizationInitiator(Integer generalization, double weight) {
        this.scissors = generalization.shortValue();

        if (weight == 0.0) {
            this.line = 0;
            this.square = 0;
            this.cube = 0;
        } else {
            this.line = (short) AlgebraUtils.integerRound(1.0 / weight);
            this.square = (short) (this.line * this.line);
            this.cube = (short) (this.square * this.line);
        }
    }

    boolean isEnabled() {
        return this.line != 0;
    }
}
