package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class NonLinearPatternGeneralization extends PatternGeneralization {

    final ProbabilityPie correlations;

    protected NonLinearPatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        super(pattern, initiator);
        this.correlations = new ProbabilityPie();
    }

    public ProbabilityPie getNonLinearData(long situation) {
        return this.correlations;
    }

}
