package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternGeneralization;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.java.HashUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ProbabilityPredictor {

    public static ProbabilityPredictor build(SpartanPlayer player,
                                             long situation,
                                             PatternGeneralization patternGeneralization,
                                             PatternProfile patternProfile,
                                             short count) {
        return new ProbabilityPredictor(
                patternGeneralization.getLinearData(player, situation, count),
                patternGeneralization.getSpecificLinearData(patternProfile, situation, count)
        );
    }

    // Separator

    public final ProbabilityPie pie;

    private ProbabilityPredictor(Collection<Collection<Double>> totalPatterns,
                                 Collection<Double> currentPattern) {
        int currentPatternSize = currentPattern.size();
        this.pie = new ProbabilityPie();

        if (!totalPatterns.isEmpty() // No general data to compare
                && currentPatternSize > 0) { // No individual data to compare
            if (currentPatternSize == 1) {
                optimized(totalPatterns, currentPattern.iterator().next());
            } else {
                long currentPatternHash = HashUtils.collection(currentPattern);

                for (Collection<Double> allPatterns : totalPatterns) {
                    boolean next = false;
                    Iterator<Double> iterator = allPatterns.iterator();
                    LinkedList<Double> list = new LinkedList<>();

                    for (int i = 0; i < currentPatternSize; i++) {
                        list.add(iterator.next());
                    }

                    while (iterator.hasNext()) {
                        double pattern = iterator.next();
                        list.add(pattern);
                        list.removeFirst();

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
        }
    }

    private void optimized(Collection<Collection<Double>> totalPatterns,
                           double currentPattern) {
        if (!totalPatterns.isEmpty()) {// No general data to compare
            long currentPatternHash = Double.hashCode(currentPattern);

            for (Collection<Double> allPatterns : totalPatterns) {
                boolean next = false;

                for (double pattern : allPatterns) {
                    if (next) {
                        next = false;
                        this.pie.add(pattern);
                    }
                    if (currentPatternHash == Double.hashCode(pattern)) {
                        next = true;
                    }
                }
            }
        }
    }

}