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

    private static final class Comparison {

        private Number number;

        private Comparison(Number number) {
            this.number = number;
        }
    }

    private final ProbabilityPie pie;
    private final ProbabilityRank rank;
    private final Map<PlayerProfile, Comparison> map;
    private final PatternCache cache;

    public PatternComparison() {
        this.pie = new ProbabilityPie();
        this.rank = new ProbabilityRank();
        this.map = new LinkedHashMap<>();
        this.cache = new PatternCache();
    }

    void put(PlayerProfile profile, Number number) {
        Comparison comparison = map.get(profile);

        if (comparison == null) {
            this.map.put(profile, new Comparison(number));
        } else {
            this.rank.remove(comparison.number);
            comparison.number = number;
        }

        if (profile.isLegitimate()) {
            this.rank.add(number);
            this.pie.add(generalizeNumber(number));

            if (cache.isExpired()) {
                cache.refresh();
                int delete = this.pie.getTotal() - Check.violationCycleTicks;

                if (delete > 0) {
                    this.pie.removeOld(delete);
                }
            }
        }
    }

    void remove(PlayerProfile profile) {
        Comparison comparison = map.remove(profile);

        if (comparison != null) {
            this.rank.remove(comparison.number);
        }
    }

    public double getPosition(Number number, double def) {
        if (!SpartanBukkit.debug && map.size() < ResearchEngine.consistentDataRequirement) {
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
