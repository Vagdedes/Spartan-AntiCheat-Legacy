package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.math.AbstractProbabilityRank;
import com.vagdedes.spartan.abstraction.math.implementation.ProbabilityPie;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PatternRankWithPie extends AbstractProbabilityRank {

    public final ProbabilityPie pie;
    private final Map<Long, Number> map;
    private final PatternCache cache;

    public PatternRankWithPie() {
        super();
        this.pie = new ProbabilityPie();
        this.map = new LinkedHashMap<>();
        this.cache = new PatternCache();
    }

    public PatternRankWithPie(int capacity) {
        super(capacity);
        this.pie = new ProbabilityPie(capacity);
        this.map = new LinkedHashMap<>(capacity);
        this.cache = new PatternCache();
    }

     public void maintain() {
        if (!map.isEmpty()) {
            if (cache.isExpired()) {
                cache.refresh();
                Iterator<Map.Entry<Long, Number>> iterator = map.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Long, Number> entry = iterator.next();

                    if (System.currentTimeMillis() - entry.getKey() > Check.violationCycleMilliseconds) {
                        iterator.remove();
                        this.pie.remove(entry.getValue());
                         super.remove(entry.getValue());
                    } else {
                        break;
                    }
                }
            }
        }
    }
}
