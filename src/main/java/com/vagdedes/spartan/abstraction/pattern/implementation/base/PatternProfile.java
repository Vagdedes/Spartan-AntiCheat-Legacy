package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;

import java.util.*;

public class PatternProfile {

    static final int
            PROBABILITY_POSITION = 0,
            SIGNIFICANCE_POSITION = 1;

    // Separator

    final Map<Integer, Collection<Double>> probability, significance;
    final Map<Integer, Collection<Long>> contribution;
    // Generalization -> Situation -> Patterns
    final Map<Integer, Map<Long, List<PatternValue>>> correlations;
    final Map<Long, PatternRepetition> repetitions;
    final double[] total;
    final PlayerProfile profile;
    private final PatternCache probabilityCache, significanceCache, contributionCache;
    public final PatternProfileStorage storage;

    PatternProfile(PatternStorage patternStorage, PlayerProfile profile) {
        this.profile = profile;
        this.storage = new PatternProfileStorage(patternStorage, this);

        this.contribution = Collections.synchronizedMap(new LinkedHashMap<>());
        this.probability = Collections.synchronizedMap(new LinkedHashMap<>());
        this.significance = Collections.synchronizedMap(new LinkedHashMap<>());
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());

        this.repetitions = new LinkedHashMap<>();

        this.probabilityCache = new PatternCache();
        this.significanceCache = new PatternCache();
        this.contributionCache = new PatternCache();

        this.total = new double[2];
        this.total[PROBABILITY_POSITION] = 0.0;
        this.total[SIGNIFICANCE_POSITION] = 0.0;
    }

    // Public

    double contributionRaw(PatternGeneralization patternGeneralization) {
        if (this.contribution.isEmpty()) {
            return -1.0;
        } else {
            Collection<Long> data = this.contribution.get(patternGeneralization.generalization);

            if (data.size() >= ResearchEngine.consistentDataRequirement) {
                Iterator<Long> iterator = data.iterator();

                if (contributionCache.isExpired()) {
                    contributionCache.refresh();
                    int delete = data.size() - Check.violationCycleTicks;

                    if (delete > 0) {
                        while (iterator.hasNext()) {
                            iterator.next();
                            iterator.remove();
                            delete--;

                            if (delete == 0) {
                                break;
                            }
                        }
                    }
                }
                int size = data.size();

                if (size >= ResearchEngine.consistentDataRequirement) {
                    long total = 0L, previous = iterator.next();

                    while (iterator.hasNext()) {
                        long current = iterator.next();

                        if (current == -1L) {
                            size--;
                        } else {
                            total += current - previous;
                            previous = current;
                        }
                    }
                    return size >= (ResearchEngine.consistentDataRequirement + 1)
                            ? total / (double) (size - 1)
                            : -1.0;
                } else {
                    return -1.0;
                }
            } else {
                return -1.0;
            }
        }
    }

    public double contribution(PatternGeneralization patternGeneralization) {
        synchronized (this.contribution) {
            return this.contributionRaw(patternGeneralization);
        }
    }

    double probabilityRaw(PatternGeneralization patternGeneralization) {
        return this.average(
                this.probability,
                probabilityCache,
                PROBABILITY_POSITION,
                patternGeneralization
        );
    }

    public double probability(PatternGeneralization patternGeneralization) {
        synchronized (this.probability) {
            return this.probabilityRaw(patternGeneralization);
        }
    }

    double significanceRaw(PatternGeneralization patternGeneralization) {
        return this.average(
                this.significance,
                significanceCache,
                SIGNIFICANCE_POSITION,
                patternGeneralization
        );
    }

    public double significance(PatternGeneralization patternGeneralization) {
        synchronized (this.significance) {
            return this.significanceRaw(patternGeneralization);
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

    // Separator

    Map<Long, List<PatternValue>> correlationsData(PatternGeneralization patternGeneralization) {
        return this.correlations.computeIfAbsent(
                patternGeneralization.generalization,
                k -> Collections.synchronizedMap(new LinkedHashMap<>())
        );
    }

    Collection<Double> probabilityData(PatternGeneralization patternGeneralization) {
        return this.probability.computeIfAbsent(
                patternGeneralization.generalization,
                k -> Collections.synchronizedList(new ArrayList<>())
        );
    }

    Collection<Double> significanceData(PatternGeneralization patternGeneralization) {
        return this.significance.computeIfAbsent(
                patternGeneralization.generalization,
                k -> Collections.synchronizedList(new ArrayList<>())
        );
    }

    Collection<Long> contributionData(PatternGeneralization patternGeneralization) {
        return this.contribution.computeIfAbsent(
                patternGeneralization.generalization,
                k -> Collections.synchronizedList(new ArrayList<>())
        );
    }

    // Private

    private double average(Map<Integer, Collection<Double>> parent,
                           PatternCache cache,
                           int index,
                           PatternGeneralization patternGeneralization) {
        if (parent.isEmpty()) {
            return -1.0;
        } else {
            Collection<Double> data = parent.get(patternGeneralization.generalization);

            if (data == null || data.size() < ResearchEngine.consistentDataRequirement) {
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
