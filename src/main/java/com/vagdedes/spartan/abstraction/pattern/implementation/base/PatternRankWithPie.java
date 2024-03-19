package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.math.AbstractProbabilityRank;
import com.vagdedes.spartan.abstraction.math.implementation.ProbabilityPie;

import java.util.Collection;
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

    @Override
    public AbstractProbabilityRank addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.map.put(System.currentTimeMillis(), number);
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
        return this;
    }

    @Override
    public AbstractProbabilityRank addMultiple(Map<? extends Number, Integer> map) {
        for (Map.Entry<? extends Number, Integer> entry : map.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                this.map.put(System.currentTimeMillis(), entry.getKey());
                this.numbers.add(entry.getKey().doubleValue());
            }
        }
        this.sorted = false;
        return this;
    }

    @Override
    public AbstractProbabilityRank addMultiplePatterns(Collection<PatternValue> values) {
        for (PatternValue value : values) {
            this.map.put(System.currentTimeMillis(), value.pattern);
            this.numbers.add(value.pattern.doubleValue());
        }
        this.sorted = false;
        return this;
    }

    @Override
    public AbstractProbabilityRank add(Number number) {
        this.map.put(System.currentTimeMillis(), number);
        return super.add(number);
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
