package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.pattern.LinearPatternProfile;
import com.vagdedes.spartan.utils.java.HashUtils;

import java.util.*;

public class ProbabilityPredictor {

    public static ProbabilityPredictor build(long situation,
                                             Collection<Double> patternData,
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
    public final Map<Integer, Double> positions;

    private ProbabilityPredictor(Collection<Double> totalPatterns,
                                 Collection<Double> currentPattern) {
        int currentPatternSize = currentPattern.size();
        this.pie = new ProbabilityPie();
        this.positions = new LinkedHashMap<>();

        if (currentPatternSize > 0
                && totalPatterns.size() > currentPatternSize) {
            long currentPatternHash = HashUtils.collection(currentPattern);
            LinkedList<Double> list = new LinkedList<>();
            Iterator<Double> iterator = totalPatterns.iterator();
            boolean next = false;
            int counter = 0;

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
                    this.positions.put(counter, pattern);
                }
                if (currentPatternHash == HashUtils.collection(list)) {
                    next = true;
                }
                counter++;
            }
        }
    }

    private ProbabilityPredictor(Collection<Double> totalPatterns,
                                 double currentPattern) {
        this.pie = new ProbabilityPie();
        this.positions = new LinkedHashMap<>();

        if (currentPattern != -1.0
                && totalPatterns.size() > 1) {
            boolean next = false;
            int counter = 0;

            for (double pattern : totalPatterns) {
                if (next) {
                    next = false;
                    this.pie.add(pattern);
                    this.positions.put(counter, pattern);
                }
                if (currentPattern == pattern) {
                    next = true;
                }
                counter++;
            }
        }
    }

}