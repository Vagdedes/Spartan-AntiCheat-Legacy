package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class NonLinearPatternGeneralization extends PatternGeneralization {

    final Map<Long, ProbabilityPie> correlations;

    protected NonLinearPatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        super(pattern, initiator);
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public ProbabilityPie getNonLinearData(long situation) {
        synchronized (this.correlations) {
            return this.correlations.getOrDefault(situation, new ProbabilityPie(0));
        }
    }

    @Override
    boolean hasSituation(long situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

}
