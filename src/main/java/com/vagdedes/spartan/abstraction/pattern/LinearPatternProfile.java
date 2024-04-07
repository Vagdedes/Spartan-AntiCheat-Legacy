package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.*;

public class LinearPatternProfile extends PatternProfile {

    final Map<Long, Collection<Double>> correlations;
    final Map<Long, Double> lastCorrelation;

    LinearPatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        super(patternGeneralization, profile);
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.lastCorrelation = new LinkedHashMap<>();
    }

    public double getLastLinearData(long situation) {
        synchronized (this.lastCorrelation) {
            return this.lastCorrelation.getOrDefault(situation, -1.0);
        }
    }

    public Collection<Double> getLinearData(long situation,
                                            short count) {
        synchronized (this.correlations) {
            Collection<Double> specific = this.correlations.get(situation);

            if (specific == null) {
                return ((LinearPatternGeneralization) this.generalization).empty;
            } else {
                int size = specific.size();

                if (size < count) {
                    return ((LinearPatternGeneralization) this.generalization).empty;
                } else if (count == 1) {
                    Double last = this.lastCorrelation.get(situation);
                    Collection<Double> collection = new ArrayList<>(1);
                    collection.add(last);
                    return collection;
                } else {
                    return new ArrayList<>(specific).subList(size - count, size);
                }
            }
        }
    }

    @Override
    void deleteCustomData() {
        synchronized (this.correlations) {
            this.correlations.clear();
        }
        synchronized (this.lastCorrelation) {
            this.lastCorrelation.clear();
        }
    }

}
