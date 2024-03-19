package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PatternComparison {

    private static final class Comparison {

        private Number number;
        private long time;

        private Comparison(Number number, long time) {
            this.number = number;
            this.time = time;
        }
    }

    public final ProbabilityPie pie;
    private final Map<PlayerProfile, Comparison> map;
    private final PatternCache cache;

    public PatternComparison() {
        this.pie = new ProbabilityPie();
        this.map = new LinkedHashMap<>();
        this.cache = new PatternCache();
    }

    void put(PlayerProfile profile, Number number, long time) {
        Comparison comparison = map.get(profile);

        if (comparison == null) {
            this.map.put(profile, new Comparison(number, time));
        } else {
            comparison.number = number;
            comparison.time = time;
        }
        this.pie.add(number);
    }

    public double getPosition(Number number, double def) {
        if (SpartanBukkit.debug && map.size() <= ResearchEngine.profileRequirement) {
            return def;
        } else {
            this.maintain();
            return 0.0;
        }
    }

    private void maintain() {
        if (!map.isEmpty()) {
            if (cache.isExpired()) {
                cache.refresh();
                Iterator<Map.Entry<PlayerProfile, Comparison>> iterator = map.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<PlayerProfile, Comparison> entry = iterator.next();

                    if (System.currentTimeMillis() - entry.getValue().time > Check.violationCycleMilliseconds) {
                        iterator.remove();
                        this.pie.remove(entry.getValue().number);
                    } else {
                        break;
                    }
                }
            }
        }
    }
}
