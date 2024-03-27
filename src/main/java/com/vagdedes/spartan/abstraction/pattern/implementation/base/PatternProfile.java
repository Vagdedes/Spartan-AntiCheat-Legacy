package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.*;

public class PatternProfile { // todo compare with little, store with a lot

    private double trustFactorTotal;
    final Collection<Double> trustFactorMemory;
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
        this.trustFactorMemory = Collections.synchronizedList(new LinkedList<>());

        this.patternRepetition = new PatternRepetition();
        this.trustFactorTotal = 0.0;
    }

    double contribution() {
        if (this.contribution.isEmpty()) {
            return -1.0;
        } else {
            if (this.contribution.size() >= this.patternGeneralization.initiator.specificWeight) {
                Iterator<Long> iterator = this.contribution.iterator();
                int size = this.contribution.size(),
                        delete = size - this.patternGeneralization.initiator.generalWeight;

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
                if (size >= this.patternGeneralization.initiator.specificWeight) {
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
                    return size >= (this.patternGeneralization.initiator.specificWeight + 1)
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

    double trustFactor() {
        if (this.trustFactorMemory.size() < this.patternGeneralization.initiator.specificWeight) {
            return -1.0;
        } else {
            int size = this.trustFactorMemory.size(),
                    delete = size - this.patternGeneralization.initiator.generalWeight;

            if (delete > 0) {
                Iterator<Double> iterator = this.trustFactorMemory.iterator();

                while (iterator.hasNext()) {
                    this.trustFactorTotal -= iterator.next();
                    iterator.remove();
                    size--;

                    if (--delete == 0) {
                        break;
                    }
                }
            }
            return this.trustFactorTotal / (double) size;
        }
    }

    void addTrustFactor(double value) {
        this.trustFactorTotal += value;
        this.trustFactorMemory.add(value);
    }
}
