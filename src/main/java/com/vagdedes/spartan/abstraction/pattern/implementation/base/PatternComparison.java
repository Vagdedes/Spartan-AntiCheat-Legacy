package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.LinkedHashMap;
import java.util.Map;

public class PatternComparison {

    private static Number generalizeNumber(Number number) {
        return PatternGeneralization.generalize(number.doubleValue(), 2);
    }

    private static final class NumberMemory {

        private Number last;
        private final ProbabilityPie pie;

        private NumberMemory(double number) {
            this.pie = new ProbabilityPie();
            this.add(number);
        }

        private void add(double number) {
            this.last = number;
            this.pie.add(generalizeNumber(number));

            if (this.pie.getTotal() > Check.violationCycleTicks) {
                this.pie.removeOld(1);
            }
        }
    }

    final String key;
    private final ProbabilityRank rank;
    private final PatternPieCache pie;
    private final Map<PatternProfile, NumberMemory> memory;

    public PatternComparison(String key) {
        this.key = key;
        this.rank = new ProbabilityRank();
        this.pie = new PatternPieCache(new ProbabilityPie());
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
                    this.memory.put(patternProfile, new NumberMemory(number));
                } else {
                    this.rank.remove(data.last);
                    data.add(number);
                }
                this.rank.add(number);

                if (this.pie.isExpired()) {
                    this.pie.refresh();
                    this.pie.data.clear();

                    for (NumberMemory value : memory.values()) {
                        this.pie.data.add(value.pie);
                    }
                }
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
                data.pie.clear();
                this.pie.data.remove(data.pie);
            }
        }
    }

    public double getPosition(Number number, double def) {
        if (!SpartanBukkit.debug && memory.size() < ResearchEngine.consistentDataRequirement) {
            return def;
        } else {
            return !SpartanBukkit.debug && this.rank.getTotal() < ResearchEngine.consistentDataRequirement
                    ? def
                    : this.rank.getProbability(number, def);
        }
    }

    public double getProbability(Number number) {
        return this.pie.data.getProbability(generalizeNumber(number));
    }
}
