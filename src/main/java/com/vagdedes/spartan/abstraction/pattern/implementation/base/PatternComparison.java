package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

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
            Number generalized = generalizeNumber(number);
            this.last = number;
            this.parent.rank.add(number);

            if (this.pie.getSlices() == this.parent.patternGeneralization.initiator.square) {
                this.parent.pie.remove(this.pie.removeLeastSignificant());
            }
            this.pie.add(generalized);
            this.parent.pie.add(generalized);
        }

    }

    final String key;
    final PatternGeneralization patternGeneralization;
    private final ProbabilityRank rank;
    private final ProbabilityPie pie;
    private final Map<PatternProfile, NumberMemory> profiles;

    PatternComparison(PatternGeneralization patternGeneralization, String key) {
        this.key = key;
        this.patternGeneralization = patternGeneralization;
        this.rank = new ProbabilityRank();
        this.pie = new ProbabilityPie();
        this.profiles = new LinkedHashMap<>();
    }

    void clear() {
        synchronized (this) {
            this.rank.clear();
            this.pie.clear();
            this.profiles.clear();
        }
    }

    void put(PatternProfile patternProfile,
             double number,
             long time,
             boolean storage) {
        if (number == -1.0) {
            synchronized (this) {
                NumberMemory data = profiles.remove(patternProfile);

                if (data != null) {
                    this.rank.remove(data.last);
                }
            }
        } else {
            synchronized (this) {
                NumberMemory data = this.profiles.get(patternProfile);

                if (data == null) {
                    this.profiles.put(patternProfile, new NumberMemory(this, number));
                } else {
                    this.rank.remove(data.last);
                    data.add(number);
                }

                if (storage) {
                    patternProfile.set(
                            this,
                            time,
                            number
                    );
                }
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
            synchronized (this) {
                return new double[]{
                        this.rank.getPosition(value),
                        this.pie.getProbability(generalizeNumber(value))
                };
            }
        }
    }

}
