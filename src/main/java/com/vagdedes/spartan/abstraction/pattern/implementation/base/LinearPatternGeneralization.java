package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import java.util.*;

public abstract class LinearPatternGeneralization extends PatternGeneralization {

    final Map<Long, Collection<Double>> correlations;
    final Collection<Double> empty;

    protected LinearPatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        super(pattern, initiator);
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.empty = new ArrayList<>(0);
    }

    public Collection<Double> getLinearData(long situation) {
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
