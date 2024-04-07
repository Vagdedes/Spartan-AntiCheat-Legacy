package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.pattern.LinearPatternGeneralization;
import com.vagdedes.spartan.abstraction.pattern.LinearPatternProfile;
import com.vagdedes.spartan.utils.java.HashUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ProbabilityPredictor {

    public static ProbabilityPredictor build(long situation,
                                             LinearPatternGeneralization patternGeneralization,
                                             LinearPatternProfile patternProfile,
                                             short count) {
        return count == 1 ? new ProbabilityPredictor(
                patternGeneralization.getLinearData(situation),
                patternProfile.getLastLinearData(situation)
        ) : new ProbabilityPredictor(
                patternGeneralization.getLinearData(situation),
                patternProfile.getLinearData(situation, count)
        );
    }

    // Separator

    public final ProbabilityPie pie;

    private ProbabilityPredictor(Collection<Double> totalPatterns,
                                 Collection<Double> currentPattern) {
        int currentPatternSize = currentPattern.size();
        this.pie = new ProbabilityPie();

        if (currentPatternSize > 0
                && totalPatterns.size() > currentPatternSize) {
            long currentPatternHash = HashUtils.collection(currentPattern);
            boolean next = false;
            Iterator<Double> iterator = totalPatterns.iterator();
            LinkedList<Double> list = new LinkedList<>();

            for (int i = 0; i < currentPatternSize; i++) {
                list.add(iterator.next());
            }

            while (iterator.hasNext()) {
                double pattern = iterator.next();
                list.removeFirst();
                list.add(pattern);

                if (next) {
                    next = false;
                    this.pie.add(pattern);
                }
                if (currentPatternHash == HashUtils.collection(list)) {
                    next = true;
                }
            }
        }
    }

    private ProbabilityPredictor(Collection<Double> totalPatterns,
                                 double currentPattern) {
        this.pie = new ProbabilityPie();

        if (currentPattern != -1.0
                && totalPatterns.size() > 1) {
            boolean next = false;

            for (double pattern : totalPatterns) {
                if (next) {
                    next = false;
                    this.pie.add(pattern);
                }
                if (currentPattern == pattern) {
                    next = true;
                }
            }
        }
    }

}