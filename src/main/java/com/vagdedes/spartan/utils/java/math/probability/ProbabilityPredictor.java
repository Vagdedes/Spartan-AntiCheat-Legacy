package com.vagdedes.spartan.utils.java.math.probability;

import com.vagdedes.spartan.utils.java.HashHelper;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProbabilityPredictor {

    private Map<Integer, ProbabilityPie> outcomes;

    public ProbabilityPredictor() {
        this(-1);
    }

    public ProbabilityPredictor(int capacity) {
        this.outcomes = capacity >= 0 ? new LinkedHashMap<>(capacity) : new LinkedHashMap<>();
    }

    public ProbabilityPie calculate(Collection<? extends Number> allPatterns,
                                    Collection<? extends Number> currentPattern,
                                    int generalize) {
        int allPatternsSize = allPatterns.size();

        if (allPatternsSize > 0) {
            int patternSize = currentPattern.size(),
                    patternHash = HashHelper.collection(currentPattern);

            if (patternSize <= allPatternsSize) {
                ProbabilityPie pie = new ProbabilityPie();
                boolean next = false, generalization = generalize > 0;
                int pos = 0;

                for (Number pattern : allPatterns) {
                    if (next) {
                        next = false;

                        if (generalization) {
                            pattern = AlgebraUtils.cut(pattern.doubleValue(), generalize);
                        }
                        pie.add(pattern);
                    }
                    if (patternHash == HashHelper.collection(allPatterns, pos - patternSize, pos)) {
                        next = true;
                    }
                    pos++;
                }
                return pie;
            }
        }
        return null;
    }
}
