package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternGeneralizationInitiator {

    // Recent individual weight is for short-term comparison when a player is online
    // General individual weight is for consideration of multiple scenarios when a player is offline
    // General weight is for long-term comparison for all players
    public final short generalization,
            line,
            square,
            cube;

    public PatternGeneralizationInitiator(Integer generalization, double weight) {
        this.generalization = generalization.shortValue();

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
