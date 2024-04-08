package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.pattern.LinearPatternProfile;
import com.vagdedes.spartan.utils.java.HashUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ProbabilityPredictor {

    public static ProbabilityPredictor build(int situation,
                                             Collection<Float> patternData,
                                             LinearPatternProfile patternProfile,
                                             short count) {
        return count == 1
                ? new ProbabilityPredictor(
                patternData,
                patternProfile.getLastLinearData(situation)
        )
                : new ProbabilityPredictor(
                patternData,
                patternProfile.getLinearData(situation, count)
        );
    }

    // Separator

    public final ProbabilityPie pie;

    private ProbabilityPredictor(Collection<Float> totalPatterns,
                                 Collection<Float> currentPattern) {
        int currentPatternSize = currentPattern.size();
        this.pie = new ProbabilityPie();

        if (currentPatternSize > 0
                && totalPatterns.size() > currentPatternSize) {
            int currentPatternHash = HashUtils.collection(currentPattern);
            LinkedList<Float> list = new LinkedList<>();
            Iterator<Float> iterator = totalPatterns.iterator();
            boolean next = false;

            for (int i = 0; i < currentPatternSize; i++) {
                list.add(iterator.next());
            }

            while (iterator.hasNext()) {
                float pattern = iterator.next();
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

    private ProbabilityPredictor(Collection<Float> totalPatterns,
                                 float currentPattern) {
        this.pie = new ProbabilityPie();

        if (currentPattern != -1.0
                && totalPatterns.size() > 1) {
            boolean next = false;

            for (float pattern : totalPatterns) {
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