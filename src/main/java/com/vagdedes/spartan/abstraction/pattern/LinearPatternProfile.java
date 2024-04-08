package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class LinearPatternProfile extends PatternProfile {

    final Map<Integer, LinkedList<Float>> correlations;

    LinearPatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        super(patternGeneralization, profile);
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    float getPreviousData(int situation) {
        synchronized (this.correlations) {
            LinkedList<Float> list = this.correlations.get(situation);

            if (list != null) {
                return list.size() <= 1 ? Float.MIN_VALUE : list.get(list.size() - 2);
            } else {
                return Float.MIN_VALUE;
            }
        }
    }

    float getLastData(int situation) {
        synchronized (this.correlations) {
            LinkedList<Float> list = this.correlations.get(situation);

            if (list != null) {
                return list.isEmpty() ? Float.MIN_VALUE : list.getLast();
            } else {
                return Float.MIN_VALUE;
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
