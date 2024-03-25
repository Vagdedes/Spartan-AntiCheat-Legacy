package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.LinkedHashMap;
import java.util.Map;

public class PatternComparison { // todo remove least significant slice of the data when it surpasses a threshold instead of removeOld

    private static Number generalizeNumber(Number number) {
        return PatternGeneralization.generalize(number.doubleValue(), 2);
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
            this.pie.add(generalized);
            this.parent.pie.add(generalized);

            if (this.pie.getTotal() > 1_000) {
                this.parent.pie.remove(this.pie.removeOld(1));
            }
        }
    }

    final String key;
    private final ProbabilityRank rank;
    private final ProbabilityPie pie;
    private final Map<PatternProfile, NumberMemory> memory;

    public PatternComparison(String key) {
        this.key = key;
        this.rank = new ProbabilityRank();
        this.pie = new ProbabilityPie();
        this.memory = new LinkedHashMap<>();
    }

    void put(PatternProfile patternProfile,
             PatternGeneralization patternGeneralization,
             double number,
             long time,
             boolean storage) {
        if (number == -1.0) {
            this.remove(patternProfile, !patternProfile.profile.isLegitimate());
        } else {
            if (patternProfile.profile.isLegitimate()) {
                NumberMemory data = memory.get(patternProfile);

                if (data == null) {
                    this.memory.put(patternProfile, new NumberMemory(this, number));
                } else {
                    this.rank.remove(data.last);
                    data.add(number);
                }
                this.rank.add(number);
            } else {
                this.remove(patternProfile, true);
            }

            if (storage) {
                patternProfile.storage.set(
                        patternGeneralization,
                        time,
                        this.key,
                        number
                );
            }
        }
    }

    private void remove(PatternProfile patternProfile, boolean pie) {
        NumberMemory data = memory.remove(patternProfile);

        if (data != null) {
            this.rank.remove(data.last);

            if (pie) {
                this.pie.remove(data.pie);
                data.pie.clear();
            }
        }
    }

    public double getPosition(Number number, double def) {
        if (!SpartanBukkit.debug && memory.size() < TPS.maximum) {
            return def;
        } else {
            return !SpartanBukkit.debug && this.rank.getTotal() < TPS.maximum
                    ? def
                    : this.rank.getProbability(number, def);
        }
    }

    public double getProbability(Number number) {
        return this.pie.getProbability(generalizeNumber(number));
    }
}
