package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NonLinearPatternGeneralization extends PatternGeneralization {

    final Map<Long, ProbabilityPie> correlations;
    private final ProbabilityPie empty;

    protected NonLinearPatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        super(pattern, initiator);
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.empty = new ProbabilityPie(0);
    }

    public ProbabilityPie getNonLinearData(long situation) {
        synchronized (this.correlations) {
            return this.correlations.getOrDefault(situation, this.empty);
        }
    }

    @Override
    boolean hasSituation(long situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

}
