package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PatternComparison {

    private static Number generalizeNumber(Number number) {
        return AlgebraUtils.generalize(number.doubleValue(), 2);
    }

    private static final class NumberMemory {

        private final PatternComparison parent;
        private Number last;
        private final ProbabilityPie pie;

        private NumberMemory(PatternComparison patternComparison, double number) {
            this.parent = patternComparison;
            this.pie = new ProbabilityPie();
            this.add(number);
        }

        private void add(double number) {
            this.last = number;
            this.parent.rank.add(number);

            Number generalized = generalizeNumber(number);
            this.pie.add(generalized);
            this.parent.pie.add(generalized);

            if (this.pie.getSlices() > this.parent.patternGeneralization.dataLimit()) {
                Iterator<Map.Entry<Number, Integer>> iterator = this.pie.removeLeastSignificant(1).iterator();

                // Remove the least significant slice if the other slice was not fully removed
                if (iterator.hasNext()
                        && !this.parent.pie.remove(iterator.next().getKey())) {
                    this.parent.pie.removeLeastSignificant(1);
                }
            }
        }

    }

    final PatternGeneralization patternGeneralization;
    final String key;
    private final ProbabilityRank rank;
    private final ProbabilityPie pie;
    private final Map<PatternProfile, NumberMemory> profiles;

    PatternComparison(PatternGeneralization patternGeneralization, String key) {
        this.patternGeneralization = patternGeneralization;
        this.key = key;
        this.rank = new ProbabilityRank();
        this.pie = new ProbabilityPie();
        this.profiles = new LinkedHashMap<>();
    }

    void clear() {
        this.rank.clear();
        this.pie.clear();
        this.profiles.clear();
    }

    void put(PatternProfile patternProfile,
             double number,
             long time,
             boolean storage) {
        if (number == -1.0) {
            this.remove(patternProfile, !patternProfile.researchProfile.isLegitimate());
        } else {
            if (patternProfile.researchProfile.isLegitimate()) {
                NumberMemory data = profiles.get(patternProfile);

                if (data == null) {
                    this.profiles.put(patternProfile, new NumberMemory(this, number));
                } else {
                    this.rank.remove(data.last);
                    data.add(number);
                }
            } else {
                this.remove(patternProfile, true);
            }

            if (storage) {
                patternProfile.storage.set(
                        this,
                        time,
                        number
                );
            }
        }
    }

    private void remove(PatternProfile patternProfile, boolean pie) {
        NumberMemory data = profiles.remove(patternProfile);

        if (data != null) {
            this.rank.remove(data.last);

            if (pie) {
                this.pie.remove(data.pie);
                data.pie.clear();
            }
        }
    }

    double[] rankAndProbability(double value) {
        if (value == -1.0
                || !SpartanBukkit.debug
                && (profiles.size() < TPS.maximum
                || this.rank.getTotal() < TPS.maximum)) {
            return null;
        } else {
            double rank = this.rank.getProbability(value, -1.0);

            if (rank != -1.0) {
                return new double[]{
                        rank,
                        this.pie.getProbability(generalizeNumber(value))
                };
            } else {
                return null;
            }
        }
    }
}
