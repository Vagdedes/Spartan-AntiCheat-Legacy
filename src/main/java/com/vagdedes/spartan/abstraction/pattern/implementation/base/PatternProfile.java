package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public class PatternProfile {

    private static final int
            dataWeight = AlgebraUtils.integerRound(TPS.maximum),
            PROBABILITY_POSITION = 0,
            SIGNIFICANCE_POSITION = 1;

    static final class MutualData {

        private final Collection<Double> probability, significance;
        private final double[] total;

        private MutualData() {
            this.probability = new LinkedList<>();
            this.significance = new LinkedList<>();

            this.total = new double[2];
            this.total[PROBABILITY_POSITION] = 0.0;
            this.total[SIGNIFICANCE_POSITION] = 0.0;
        }

        void add(double probability, double significance) {
            this.total[PatternProfile.PROBABILITY_POSITION] += probability;
            this.total[PatternProfile.SIGNIFICANCE_POSITION] += significance;
            this.probability.add(probability);
            this.significance.add(significance);
        }
    }

    // Separator

    final Map<Integer, MutualData> mutual;
    final Map<Integer, Collection<Long>> contribution;
    // Generalization -> Situation -> Patterns
    final Map<Integer, Map<Long, List<PatternValue>>> correlations;
    final Map<Long, PatternRepetition> repetitions;
    final PlayerProfile profile;
    final Pattern parent;
    final PatternProfileStorage storage;

    PatternProfile(Pattern patternStorage, PlayerProfile profile) {
        this.parent = patternStorage;
        this.profile = profile;
        this.storage = new PatternProfileStorage(this);

        this.correlations = new LinkedHashMap<>();
        this.repetitions = new LinkedHashMap<>();

        this.contribution = Collections.synchronizedMap(new LinkedHashMap<>());
        this.mutual = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    // Public

    double contributionRaw(PatternGeneralization patternGeneralization) {
        if (this.contribution.isEmpty()) {
            return -1.0;
        } else {
            Collection<Long> data = this.contribution.get(patternGeneralization.generalization);

            if (data.size() >= ResearchEngine.consistentDataRequirement) {
                Iterator<Long> iterator = data.iterator();
                int size = data.size(),
                        delete = size - dataWeight;

                if (delete > 0) {
                    while (iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();
                        size--;

                        if (--delete == 0) {
                            break;
                        }
                    }
                }
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
                this.mutual,
                PROBABILITY_POSITION,
                patternGeneralization
        );
    }

    public double probability(PatternGeneralization patternGeneralization) {
        synchronized (this.mutual) {
            return this.probabilityRaw(patternGeneralization);
        }
    }

    double significanceRaw(PatternGeneralization patternGeneralization) {
        return this.average(
                this.mutual,
                SIGNIFICANCE_POSITION,
                patternGeneralization
        );
    }

    public double significance(PatternGeneralization patternGeneralization) {
        synchronized (this.mutual) {
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
                k -> new LinkedHashMap<>()
        );
    }

    MutualData mutualData(PatternGeneralization patternGeneralization) {
        return this.mutual.computeIfAbsent(
                patternGeneralization.generalization,
                k -> new MutualData()
        );
    }

    Collection<Long> contributionData(PatternGeneralization patternGeneralization) {
        return this.contribution.computeIfAbsent(
                patternGeneralization.generalization,
                k -> new LinkedList<>()
        );
    }

    // Private

    private double average(Map<Integer, MutualData> parent,
                           int index,
                           PatternGeneralization patternGeneralization) {
        if (parent.isEmpty()) {
            return -1.0;
        } else {
            MutualData data = parent.get(patternGeneralization.generalization);

            if (data == null) {
                return -1.0;
            } else {
                Collection<Double> collection =
                        index == PROBABILITY_POSITION
                                ? data.probability
                                : data.significance;

                if (collection.size() < ResearchEngine.consistentDataRequirement) {
                    return -1.0;
                } else {
                    int delete = collection.size() - dataWeight;

                    if (delete > 0) {
                        Iterator<Double> iterator = collection.iterator();

                        while (iterator.hasNext()) {
                            data.total[index] -= iterator.next();
                            iterator.remove();

                            if (--delete == 0) {
                                break;
                            }
                        }
                    }
                    return data.total[index] / (double) collection.size();
                }
            }
        }
    }

}
