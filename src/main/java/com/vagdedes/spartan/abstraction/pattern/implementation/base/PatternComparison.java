package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.LinkedHashMap;
import java.util.Map;

public class PatternComparison {

    private final ProbabilityPie pie;
    private final ProbabilityRank rank;
    private final Map<PlayerProfile, Number> rankMemory;
    private final PatternCache cache;

    public PatternComparison() {
        this.pie = new ProbabilityPie();
        this.rank = new ProbabilityRank();
        this.rankMemory = new LinkedHashMap<>();
        this.cache = new PatternCache();
    }

    void put(PlayerProfile profile, Number number) {
        Number previous = rankMemory.get(profile);

        if (previous == null) {
            this.rankMemory.put(profile, number);
        } else {
            this.rank.remove(previous);
            this.rankMemory.put(profile, number);
        }

        if (profile.isLegitimate()) {
            this.rank.add(number);
            this.pie.add(generalizeNumber(number));

            if (cache.isExpired()) {
                cache.refresh();
                int delete = this.pie.getTotal() - (Check.violationCycleTicks * ResearchEngine.consistentDataRequirement);

                if (delete > 0) {
                    this.pie.removeOld(delete);
                }
            }
        }
    }

    void remove(PlayerProfile profile) {
        Number number = rankMemory.remove(profile);

        if (number != null) {
            this.rank.remove(number);
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
