package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.*;

public class LinearPatternProfile extends PatternProfile {

    final Map<Long, Collection<Double>> correlations;
    final Map<Long, Double> lastCorrelation;
    private final Collection<Double> empty;

    LinearPatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        super(patternGeneralization, profile);
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.lastCorrelation = new LinkedHashMap<>();
        this.empty = new ArrayList<>(0);
    }

    public double getLastLinearData(long situation) {
        synchronized (this.lastCorrelation) {
            return this.lastCorrelation.getOrDefault(situation, -1.0);
        }
    }

    public Collection<Double> getLinearData(long situation, short count) {
        synchronized (this.correlations) {
            Collection<Double> specific = this.correlations.get(situation);

            if (specific == null) {
                return this.empty;
            } else {
                int size = specific.size();

                if (size < count) {
                    return this.empty;
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
