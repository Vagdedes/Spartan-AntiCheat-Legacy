package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternGeneralizationInitiator {

    final short
            scissors,
            line,
            square,
            cube;

    public PatternGeneralizationInitiator(Integer generalization, double weight) {
        this.scissors = generalization.shortValue();
        this.line = (short) AlgebraUtils.integerRound(1.0 / weight);
        this.square = (short) (this.line * this.line);
        this.cube = (short) (this.square * this.line);
    }
}
