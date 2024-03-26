package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public class PatternProfile { // todo limit storage to the weight in one file named by the user

    // todo store more weight in the file and use it when the player is offline but not when the player is online
    // todo connect object to generalization instead of the pattern and use it to set the weight for each generalization

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

    final MutualData mutual;
    final Collection<Long> contribution;
    // Generalization -> Situation -> Patterns
    final Map<Long, List<PatternValue>> correlations;
    final PatternRepetition patternRepetition;
    final PlayerProfile researchProfile;
    final PatternGeneralization patternGeneralization;
    final PatternGeneralizationFile storage;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.patternGeneralization = patternGeneralization;
        this.researchProfile = profile;
        this.storage = new PatternGeneralizationFile(this);

        this.correlations = new LinkedHashMap<>();
        this.contribution = new LinkedList<>();

        this.patternRepetition = new PatternRepetition();
        this.mutual = new MutualData();
    }

    // Public

    double contributionRaw() {
        if (this.contribution.isEmpty()) {
            return -1.0;
        } else {
            if (this.contribution.size() >= dataWeight) {
                Iterator<Long> iterator = this.contribution.iterator();
                int size = this.contribution.size(),
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
                if (size >= dataWeight) {
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
                    return size >= (dataWeight + 1)
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

    public double contribution() {
        synchronized (this.contribution) {
            return this.contributionRaw();
        }
    }

    double probabilityRaw() {
        return this.average(PROBABILITY_POSITION);
    }

    public double probability() {
        synchronized (this.mutual) {
            return this.probabilityRaw();
        }
    }

    double significanceRaw() {
        return this.average(SIGNIFICANCE_POSITION);
    }

    public double significance() {
        synchronized (this.mutual) {
            return this.significanceRaw();
        }
    }

    // Private

    private double average(int index) {
        Collection<Double> collection =
                index == PROBABILITY_POSITION
                        ? mutual.probability
                        : mutual.significance;

        if (collection.size() < dataWeight) {
            return -1.0;
        } else {
            int delete = collection.size() - dataWeight;

            if (delete > 0) {
                Iterator<Double> iterator = collection.iterator();

                while (iterator.hasNext()) {
                    mutual.total[index] -= iterator.next();
                    iterator.remove();

                    if (--delete == 0) {
                        break;
                    }
                }
            }
            return mutual.total[index] / (double) collection.size();
        }
    }

}
