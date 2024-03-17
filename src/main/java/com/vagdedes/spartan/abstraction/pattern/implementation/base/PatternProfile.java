package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PatternProfile {

    final List<Long> failures, newData, existingData;
    final Map<Long, Double> successProbability, successSignificance;
    // Generalization -> Situation -> Patterns
    final Map<Integer, Map<Long, List<PatternValue>>> correlations;
    final Map<Long, PatternRepetition> repetitions;

    PatternProfile() {
        this.failures = new ArrayList<>();
        this.newData = new ArrayList<>();
        this.existingData = new ArrayList<>();
        this.successProbability = new LinkedHashMap<>();
        this.successSignificance = new LinkedHashMap<>();
        this.correlations = new LinkedHashMap<>();
        this.repetitions = new LinkedHashMap<>();
    }

    int failures() {
        return this.failures.size();
    }

    // Separator

    double contribution() {
        return this.existingData.isEmpty()
                ? this.newData.size()
                : this.newData.size() / (double) this.existingData.size();
    }

    double probability() {
        if (this.successProbability.isEmpty()) {
            return 0.0;
        } else {
            double count = 0.0;

            for (double probability : this.successProbability.values()) {
                count += probability;
            }
            return count / (double) this.successProbability.size();
        }
    }

    double significance() {
        if (this.successSignificance.isEmpty()) {
            return 0.0;
        } else {
            double count = 0.0;

            for (double significance : this.successSignificance.values()) {
                count += significance;
            }
            return count / (double) this.successSignificance.size();
        }
    }

    // Separator

    boolean canUsePattern(long situation, Number number) {
        PatternRepetition repetition = this.repetitions.get(situation);

        if (repetition == null) {
            this.repetitions.put(situation, new PatternRepetition(number));
            return true;
        } else {
            return repetition.canUse(number);
        }
    }
}
