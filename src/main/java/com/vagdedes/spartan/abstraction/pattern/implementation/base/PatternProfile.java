package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;

import java.util.*;

public class PatternProfile { // todo store

    static final int
            PROBABILITY_POSITION = 0,
            SIGNIFICANCE_POSITION = 1;

    final Map<Integer, Collection<Double>> probability, significance;
    final Map<Integer, Collection<Long>> contribution;
    // Generalization -> Situation -> Patterns
    final Map<Integer, Map<Long, List<PatternValue>>> correlations;
    final Map<Long, PatternRepetition> repetitions;
    private final PatternCache probabilityCache, significanceCache, contributionCache;
    final double[] total;

    PatternProfile() {
        this.contribution = Collections.synchronizedMap(new LinkedHashMap<>());
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
        if (this.contribution.isEmpty()) {
            return -1.0;
        } else {
            Collection<Long> data = this.contribution.get(generalization);

            if (data.size() > 1) {
                Iterator<Long> iterator = data.iterator();

                if (contributionCache.isExpired()) {
                    contributionCache.refresh();
                    int delete = data.size() - Check.violationCycleTicks;

                    if (delete > 0) {
                        while (iterator.hasNext()) {
                            iterator.remove();
                            delete--;

                            if (delete == 0) {
                                break;
                            }
                        }
                    }
                }
                long total = 0L, previous = iterator.next();

                while (iterator.hasNext()) {
                    long current = iterator.next();
                    total += current - previous;
                    previous = current;
                }
                return total / (double) (data.size() - 1);
            } else {
                return -1.0;
            }
        }
    }

    public double contribution(int generalization) {
        synchronized (this.contribution) {
            return this.contributionRaw(generalization);
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

    private double average(Map<Integer, Collection<Double>> parent,
                           PatternCache cache,
                           int index,
                           int generalization) {
        if (parent.isEmpty()) {
            return -1.0;
        } else {
            Collection<Double> data = parent.get(generalization);

            if (data == null || data.isEmpty()) {
                return -1.0;
            } else {
                if (cache.isExpired()) {
                    cache.refresh();
                    int delete = data.size() - Check.violationCycleTicks;

                    if (delete > 0) {
                        Iterator<Double> iterator = data.iterator();

                        while (iterator.hasNext()) {
                            this.total[index] -= iterator.next();
                            iterator.remove();
                            delete--;

                            if (delete == 0) {
                                break;
                            }
                        }
                    }
                }
                return this.total[index] / (double) data.size();
            }
        }
    }
}
