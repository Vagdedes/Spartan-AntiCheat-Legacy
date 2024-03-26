package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public class PatternProfile { // todo compare with little, store with a lot

    private static final int
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

    final MutualData mutualData;
    final Collection<Long> contribution;
    final Map<Long, List<PatternValue>> correlations;
    final PatternRepetition patternRepetition;
    final PlayerProfile researchProfile;
    final PatternGeneralization patternGeneralization;
    final PatternProfileStorage storage;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.patternGeneralization = patternGeneralization;
        this.researchProfile = profile;
        this.storage = new PatternProfileStorage(this);

        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.contribution = Collections.synchronizedList(new LinkedList<>());

        this.patternRepetition = new PatternRepetition();
        this.mutualData = new MutualData();
    }

    double contribution() {
        if (this.contribution.isEmpty()) {
            return -1.0;
        } else {
            if (this.contribution.size() >= this.patternGeneralization.initiator.onlineWeight) {
                Iterator<Long> iterator = this.contribution.iterator();
                int size = this.contribution.size(),
                        delete = size - this.patternGeneralization.initiator.offlineWeight;

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
                if (size >= this.patternGeneralization.initiator.onlineWeight) {
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
                    return size >= (this.patternGeneralization.initiator.onlineWeight + 1)
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

    double probability() {
        return this.average(PROBABILITY_POSITION);
    }

    double significance() {
        return this.average(SIGNIFICANCE_POSITION);
    }

    // Separator

    private double average(int index) {
        Collection<Double> collection =
                index == PROBABILITY_POSITION
                        ? mutualData.probability
                        : mutualData.significance;

        if (collection.size() < this.patternGeneralization.initiator.onlineWeight) {
            return -1.0;
        } else {
            int delete = collection.size() - this.patternGeneralization.initiator.offlineWeight;

            if (delete > 0) {
                Iterator<Double> iterator = collection.iterator();

                while (iterator.hasNext()) {
                    mutualData.total[index] -= iterator.next();
                    iterator.remove();

                    if (--delete == 0) {
                        break;
                    }
                }
            }
            return mutualData.total[index] / (double) collection.size();
        }
    }

}
