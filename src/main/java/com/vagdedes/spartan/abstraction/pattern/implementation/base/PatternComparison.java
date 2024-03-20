package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.LinkedHashMap;
import java.util.Map;

public class PatternComparison { // todo limit influence in data

    final String key;
    private final ProbabilityPie pie;
    private final ProbabilityRank rank;
    private final Map<PatternProfile, Number> rankMemory;
    private final PatternCache cache;

    public PatternComparison(String key) {
        this.key = key;
        this.pie = new ProbabilityPie();
        this.rank = new ProbabilityRank();
        this.rankMemory = new LinkedHashMap<>();
        this.cache = new PatternCache();
    }

    void put(PatternProfile patternProfile,
             PatternGeneralization patternGeneralization,
             double number,
             long time,
             boolean storage) {
        if (number == -1.0) {
            this.remove(patternProfile, !patternProfile.profile.isLegitimate());
        } else {
            Number previous = rankMemory.get(patternProfile);

            if (previous == null) {
                this.rankMemory.put(patternProfile, number);
            } else {
                this.rank.remove(previous);
                this.rankMemory.put(patternProfile, number);
            }

            if (patternProfile.profile.isLegitimate()) {
                this.rank.add(number);
                this.pie.add(generalizeNumber(number));

                if (cache.isExpired()) {
                    cache.refresh();
                    int delete = this.pie.getTotal() - (Check.violationCycleTicks * ResearchEngine.consistentDataRequirement);

                    if (delete > 0) {
                        this.pie.removeOld(delete);
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
        Number previous = rankMemory.remove(patternProfile);

        if (previous != null) {
            this.rank.remove(previous);

            if (pie) {
                this.pie.remove(previous);
            }
        }
    }

    public double getPosition(Number number, double def) {
        if (!SpartanBukkit.debug && rankMemory.size() < ResearchEngine.consistentDataRequirement) {
            return def;
        } else {
            return !SpartanBukkit.debug && this.rank.getTotal() < ResearchEngine.consistentDataRequirement
                    ? def
                    : this.rank.getProbability(number, def);
        }
    }

    public double getProbability(Number number) {
        return this.pie.getProbability(generalizeNumber(number));
    }

    private Number generalizeNumber(Number number) {
        return PatternGeneralization.generalize(number.doubleValue(), 2);
    }
}
