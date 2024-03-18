package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PatternProfile {

    final Map<Integer, List<Long>> newData; // Local, Publicly accessed
    final Map<Integer, Map<Long, Double>> probability, significance; // Local, Publicly accessed
    // Generalization -> Situation -> Patterns
    final Map<Integer, Map<Long, List<PatternValue>>> correlations; // Local
    final Map<Long, PatternRepetition> repetitions; // Local

    PatternProfile() {
        this.newData = new LinkedHashMap<>();
        this.probability = new LinkedHashMap<>();
        this.significance = new LinkedHashMap<>();
        this.correlations = new LinkedHashMap<>();
        this.repetitions = new LinkedHashMap<>();
    }

    // Public

    public double contribution(int generalization) {
        if (this.significance.isEmpty()) {
            return 0.0;
        } else {
            int map;

            synchronized (this.significance) {
                map = this.countMap(this.significance, generalization);
            }

            if (map == 0) {
                return 0.0;
            } else {
                int collection;

                synchronized (this.newData) {
                    collection = this.countCollection(this.newData, generalization);
                }
                return collection / (double) map;
            }
        }
    }

    public double probability(int generalization) {
        synchronized (this.probability) {
            return this.average(this.probability, generalization);
        }
    }

    public double significance(int generalization) {
        synchronized (this.significance) {
            return this.average(this.significance, generalization);
        }
    }

    // Local

    boolean canUsePattern(long situation, Number number) {
        PatternRepetition repetition = this.repetitions.get(situation);

        if (repetition == null) {
            this.repetitions.put(situation, new PatternRepetition(number));
            return true;
        } else {
            return repetition.canUse(number);
        }
    }

    // Private

    private double average(Map<Integer, Map<Long, Double>> parent, int generalization) {
        if (parent.isEmpty()) {
            return 0.0;
        } else {
            Map<Long, Double> map = parent.get(generalization);

            if (map == null) {
                return 0.0;
            } else {
                double count = 0.0;

                for (double significance : map.values()) {
                    count += significance;
                }
                return count / (double) map.size();
            }
        }
    }

    private <K, V> int countMap(Map<Integer, ? extends Map<K, V>> parent, int generalization) {
        if (parent.isEmpty()) {
            return 0;
        } else {
            Map<K, V> map = parent.get(generalization);
            return map == null ? 0 : map.size();
        }
    }

    private <E> int countCollection(Map<Integer, ? extends Collection<E>> parent, int generalization) {
        if (parent.isEmpty()) {
            return 0;
        } else {
            Collection<E> map = parent.get(generalization);
            return map == null ? 0 : map.size();
        }
    }
}
