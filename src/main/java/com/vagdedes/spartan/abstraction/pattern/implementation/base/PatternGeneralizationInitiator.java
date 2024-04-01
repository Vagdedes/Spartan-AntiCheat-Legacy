package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternGeneralizationInitiator {

    // Recent individual weight is for short-term comparison when a player is online
    // General individual weight is for consideration of multiple scenarios when a player is offline
    // General weight is for long-term comparison for all players
    public final int generalization,
            line,
            square,
            cube;

    public PatternGeneralizationInitiator(int generalization, double weight) {
        this.generalization = generalization;
        this.line = AlgebraUtils.integerRound(1.0 / weight);
        this.square = this.line * this.line;
        this.cube = this.square * this.line;
    }

}
