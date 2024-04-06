package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class LinearPatternGeneralization extends PatternGeneralization {

    final Map<Long, Collection<Double>> correlations;

    protected LinearPatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        super(pattern, initiator);
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public Collection<Double> getLinearData(long situation) {
        synchronized (this.correlations) {
            return this.correlations.getOrDefault(situation, super.empty);
        }
    }

}
