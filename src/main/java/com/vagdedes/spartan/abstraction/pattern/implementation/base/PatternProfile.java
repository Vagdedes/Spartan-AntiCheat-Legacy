package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;

import java.util.*;

public class PatternProfile {

    static final int
            PROBABILITY_POSITION = 0,
            SIGNIFICANCE_POSITION = 1;

    final Map<Integer, Collection<Long>> newData;
    final Map<Integer, Map<Long, Double>> probability, significance;
    // Generalization -> Situation -> Patterns
    final Map<Integer, Map<Long, List<PatternValue>>> correlations;
    final Map<Long, PatternRepetition> repetitions;
    private final PatternCache probabilityCache,
            significanceCache,
            contributionCache;
    final double[] total;

    PatternProfile() {
        this.newData = Collections.synchronizedMap(new LinkedHashMap<>());
        this.probability = Collections.synchronizedMap(new LinkedHashMap<>());
        this.significance = Collections.synchronizedMap(new LinkedHashMap<>());

        this.correlations = new LinkedHashMap<>();
        this.repetitions = new LinkedHashMap<>();

        this.probabilityCache = new PatternCache();
        this.significanceCache = new PatternCache();
        this.contributionCache = new PatternCache();

        this.total = new double[2];
        this.total[PROBABILITY_POSITION] = 0.0;
        this.total[SIGNIFICANCE_POSITION] = 0.0;
    }

    // Public

    double contributionRaw(int generalization) {
        if (this.significance.isEmpty()) {
            return 0.0;
        } else {
            int map;

            synchronized (this.significance) {
                map = this.countMap(this.significance, significanceCache, SIGNIFICANCE_POSITION, generalization);
            }

            if (map == 0) {
                return 0.0;
            } else {
                return this.countCollection(this.newData, contributionCache, generalization) / (double) map;
            }
        }
    }

    public double contribution(int generalization) {
        if (this.significance.isEmpty()) {
            return 0.0;
        } else {
            int map;

            synchronized (this.significance) {
                map = this.countMap(this.probability, probabilityCache, PROBABILITY_POSITION, generalization);
            }

            if (map == 0) {
                return 0.0;
            } else {
                int collection;

                synchronized (this.newData) {
                    collection = this.countCollection(this.newData, contributionCache, generalization);
                }
                return collection / (double) map;
            }
        }
    }

    double probabilityRaw(int generalization) {
        return this.average(this.probability, probabilityCache, PROBABILITY_POSITION, generalization);
    }

    public double probability(int generalization) {
        synchronized (this.probability) {
            return this.probabilityRaw(generalization);
        }
    }

    double significanceRaw(int generalization) {
        return this.average(this.significance, significanceCache, SIGNIFICANCE_POSITION, generalization);
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
                           PatternCache cache,
                           int index,
                           int generalization) {
        if (parent.isEmpty()) {
            return 0.0;
        } else {
            Map<Long, Double> map = parent.get(generalization);

            if (map == null || map.isEmpty()) {
                return 0.0;
            } else if (cache.isExpired()) {
                cache.refresh();
                double count = 0.0;
                Iterator<Map.Entry<Long, Double>> iterator = map.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Long, Double> entry = iterator.next();

                    if (System.currentTimeMillis() - entry.getKey() > Check.violationCycleMilliseconds) {
                        iterator.remove();
                        this.total[index] -= entry.getValue();
                    } else {
                        break;
                    }
                }
                while (iterator.hasNext()) {
                    count += iterator.next().getValue();
                }
                return map.isEmpty()
                        ? 0.0
                        : count / (double) map.size();
            } else {
                return this.total[index] / (double) map.size();
            }
        }
    }

    private int countMap(Map<Integer, Map<Long, Double>> parent,
                         PatternCache cache,
                         int index,
                         int generalization) {
        if (parent.isEmpty()) {
            return 0;
        } else {
            Map<Long, Double> map = parent.get(generalization);

            if (map == null || map.isEmpty()) {
                return 0;
            } else {
                if (cache.isExpired()) {
                    cache.refresh();
                    Iterator<Map.Entry<Long, Double>> iterator = map.entrySet().iterator();

                    while (iterator.hasNext()) {
                        Map.Entry<Long, Double> entry = iterator.next();

                        if (System.currentTimeMillis() - entry.getKey() > Check.violationCycleMilliseconds) {
                            iterator.remove();
                            this.total[index] -= entry.getValue();
                        } else {
                            break;
                        }
                    }
                }
                return map.size();
            }
        }
    }

    private int countCollection(Map<Integer, Collection<Long>> parent,
                                PatternCache cache,
                                int generalization) {
        if (parent.isEmpty()) {
            return 0;
        } else {
            Collection<Long> map = parent.get(generalization);

            if (map == null || map.isEmpty()) {
                return 0;
            } else {
                if (cache.isExpired()) {
                    cache.refresh();
                    Iterator<Long> iterator = map.iterator();

                    while (iterator.hasNext()) {
                        if (System.currentTimeMillis() - iterator.next() > Check.violationCycleMilliseconds) {
                            iterator.remove();
                        } else {
                            break;
                        }
                    }
                }
                return map.size();
            }
        }
    }
}
