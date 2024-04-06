package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.*;

public class LinearPatternProfile extends PatternProfile {

    final Map<Long, Collection<Double>> correlations;

    LinearPatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        super(patternGeneralization, profile);
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
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
                } else if (count == 1 && specific instanceof LinkedList) {
                    Collection<Double> collection = new ArrayList<>(1);
                    collection.add(((LinkedList<Double>) specific).getLast());
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
    }

}
