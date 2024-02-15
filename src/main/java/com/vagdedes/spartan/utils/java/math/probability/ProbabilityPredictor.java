package com.vagdedes.spartan.utils.java.math.probability;

import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.HashHelper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProbabilityPredictor {

    private final Map<Long, ProbabilityPie> outcomes;

    public ProbabilityPredictor() {
        this(-1);
    }

    public ProbabilityPredictor(int capacity) {
        this.outcomes = capacity >= 0 ? new LinkedHashMap<>(capacity) : new LinkedHashMap<>();
    }

    public ProbabilityPie calculate(Collection<? extends Number> allPatterns,
                                    Collection<? extends Number> currentPattern) {
        int allPatternsSize = allPatterns.size();

        if (allPatternsSize > 0) {
            int patternSize = currentPattern.size();
            long patternHash = HashHelper.collection(currentPattern);

            if (patternSize <= allPatternsSize) {
                long hash = (HashHelper.fastCollection(allPatterns) * SpartanBukkit.hashCodeMultiplierLong)
                        + patternHash;
                ProbabilityPie pie = this.outcomes.get(hash);

                if (pie != null) {
                    return pie;
                } else {
                    pie = new ProbabilityPie();
                    boolean next = false;
                    int pos = 0;
                    for (Number pattern : allPatterns) {
                        if (next) {
                            next = false;
                            pie.add(pattern);
                        }
                        if (patternHash == HashHelper.collection(allPatterns, pos - patternSize, pos)) {
                            next = true;
                        }
                        pos++;
                    }
                    this.outcomes.put(hash, pie);
                    return pie;
                }
            }
        }
        return null;
    }
}
