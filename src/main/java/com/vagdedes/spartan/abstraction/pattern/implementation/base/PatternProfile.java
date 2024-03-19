package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public class PatternProfile {

    final Map<Integer, Collection<Long>> newData; // Local, Publicly accessed
    final Map<Integer, Map<Long, Double>> probability, significance; // Local, Publicly accessed
    // Generalization -> Situation -> Patterns
    final Map<Integer, Map<Long, List<PatternValue>>> correlations; // Local
    final Map<Long, PatternRepetition> repetitions; // Local
    private final PatternProfileCache probabilityCache, // Private
            significanceCache, // Private
            contributionCache; // Private

    PatternProfile() {
        this.newData = Collections.synchronizedMap(new LinkedHashMap<>());
        this.probability = Collections.synchronizedMap(new LinkedHashMap<>());
        this.significance = Collections.synchronizedMap(new LinkedHashMap<>());
        this.correlations = new LinkedHashMap<>();
        this.repetitions = new LinkedHashMap<>();
        this.probabilityCache = new PatternProfileCache();
        this.significanceCache = new PatternProfileCache();
        this.contributionCache = new PatternProfileCache();
    }

    // Public

    double contributionRaw(int generalization) { // 4 because that's the preferable length of a percentage
        if (this.significance.isEmpty()) {
            return 0.0;
        } else {
            int map;

            synchronized (this.significance) {
                map = this.countMap(this.significance, significanceCache, generalization);
            }

            if (map == 0) {
                return 0.0;
            } else {
                return AlgebraUtils.cut(
                        this.countCollection(this.newData, contributionCache, generalization) / (double) map,
                        4
                );
            }
        }
    }

    public double contribution(int generalization) { // 4 because that's the preferable length of a percentage
        if (this.significance.isEmpty()) {
            return 0.0;
        } else {
            int map;

            synchronized (this.significance) {
                map = this.countMap(this.significance, significanceCache, generalization);
            }

            if (map == 0) {
                return 0.0;
            } else {
                int collection;

                synchronized (this.newData) {
                    collection = this.countCollection(this.newData, contributionCache, generalization);
                }
                return AlgebraUtils.cut(collection / (double) map, 4);
            }
        }
    }

    double probabilityRaw(int generalization) { // 4 because that's the preferable length of a percentage
        return AlgebraUtils.cut(this.average(this.probability, probabilityCache, generalization), 4);
    }

    public double probability(int generalization) {
        synchronized (this.probability) {
            return this.probabilityRaw(generalization);
        }
    }

    double significanceRaw(int generalization) { // 4 because that's the preferable length of a percentage
        return AlgebraUtils.cut(this.average(this.significance, significanceCache, generalization), 4);
    }

    public double significance(int generalization) {
        synchronized (this.significance) {
            return this.significanceRaw(generalization);
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

    private double average(Map<Integer, Map<Long, Double>> parent,
                           PatternProfileCache cache,
                           int generalization) {
        if (parent.isEmpty()) {
            return 0.0;
        } else {
            Map<Long, Double> map = parent.get(generalization);

            if (map == null || map.isEmpty()) {
                return 0.0;
            } else {
                this.remove(map.keySet().iterator(), cache);
                double count = 0.0;

                for (double significance : map.values()) {
                    count += significance;
                }
                return count / (double) map.size();
            }
        }
    }

    private int countMap(Map<Integer, Map<Long, Double>> parent,
                         PatternProfileCache cache,
                         int generalization) {
        if (parent.isEmpty()) {
            return 0;
        } else {
            Map<Long, Double> map = parent.get(generalization);

            if (map == null || map.isEmpty()) {
                return 0;
            } else {
                this.remove(map.keySet().iterator(), cache);
                return map.size();
            }
        }
    }

    private int countCollection(Map<Integer, Collection<Long>> parent,
                                PatternProfileCache cache,
                                int generalization) {
        if (parent.isEmpty()) {
            return 0;
        } else {
            Collection<Long> map = parent.get(generalization);

            if (map == null || map.isEmpty()) {
                return 0;
            } else {
                this.remove(map.iterator(), cache);
                return map.size();
            }
        }
    }

    private void remove(Iterator<Long> iterator, PatternProfileCache cache) {
        if (cache.isExpired()) {
            cache.refresh();

            while (iterator.hasNext()) {
                if (System.currentTimeMillis() - iterator.next() > Check.violationCycleMilliseconds) {
                    iterator.remove();
                }
            }
        }
    }
}
