package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternGeneralizationInitiator {

    // Specific weight is for short-term comparison when a player is online
    // General weight is for consideration of multiple scenarios when a player is offline
    public final int generalization,
            specificWeight, generalWeight,
            individualIterations, generalIterations;

    public PatternGeneralizationInitiator(int generalization, double weight, int iterations) {
        this.generalization = generalization;
        this.specificWeight = AlgebraUtils.integerRound(1.0 / weight);
        this.generalWeight = this.specificWeight * this.specificWeight;
        this.individualIterations = iterations;
        this.generalIterations = iterations * 10;
    }

    public PatternGeneralizationInitiator(double weight, int iterations) {
        this(0, weight, iterations);
    }

}
