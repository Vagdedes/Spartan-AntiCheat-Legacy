package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class PatternGeneralizationInitiator {

    public final int generalization, onlineWeight, offlineWeight;

    public PatternGeneralizationInitiator(int generalization, double weight) {
        this.generalization = generalization;
        this.onlineWeight = AlgebraUtils.integerRound(1.0 / weight);
        this.offlineWeight = Check.violationCycleTicks;
    }

}
