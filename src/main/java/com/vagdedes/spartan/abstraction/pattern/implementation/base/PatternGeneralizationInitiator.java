package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternGeneralizationInitiator {

    public final int generalization, specificWeight, generalWeight;

    public PatternGeneralizationInitiator(int generalization, double weight) {
        this.generalization = generalization;
        this.specificWeight = AlgebraUtils.integerRound(1.0 / weight);
        this.generalWeight = this.specificWeight * this.specificWeight;
    }

}
