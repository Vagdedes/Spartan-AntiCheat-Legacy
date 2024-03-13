package com.vagdedes.spartan.utils.math.probability;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternValue;
import com.vagdedes.spartan.utils.java.HashHelper;

import java.util.Collection;

public class ProbabilityPredictor {

    public final ProbabilityPie pie;
    public final double patternSignificance;

    public ProbabilityPredictor(Collection<Collection<PatternValue>> totalPatterns,
                                Collection<PatternValue> currentPattern) {
        this.pie = new ProbabilityPie();

        if (!totalPatterns.isEmpty()) {
            long currentPatternHash = HashHelper.collection(currentPattern);
            int currentPatternSize = currentPattern.size(),
                    totalPatternSize = 0,
                    currentPatternCount = 0;

            for (Collection<PatternValue> allPatterns : totalPatterns) {
                int allPatternsSize = allPatterns.size();

                if (currentPatternSize <= allPatternsSize) {
                    totalPatternSize += allPatternsSize;
                    boolean next = false;
                    int pos = currentPatternSize;

                    for (PatternValue pattern : allPatterns) {
                        if (next) {
                            next = false;
                            currentPatternCount++;
                            this.pie.add(pattern.pattern);
                        }
                        if (currentPatternHash == HashHelper.collection(allPatterns, pos - currentPatternSize, pos)) {
                            next = true;
                        }
                        pos++;
                    }
                }
            }
            if (totalPatternSize > 0) {
                this.patternSignificance = (currentPatternCount * currentPatternSize) / (double) totalPatternSize;
            } else {
                this.patternSignificance = 0.0;
            }
        } else {
            this.patternSignificance = 0.0;
        }
    }
}
