package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.math.AbstractProbabilityRank;
import com.vagdedes.spartan.utils.math.probability.ProbabilityPie;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PatternRankWithPie extends AbstractProbabilityRank {

    public final ProbabilityPie pie;
    private final Map<Long, Number> map;

    public PatternRankWithPie() {
        super();
        this.pie = new ProbabilityPie();
        this.map = new LinkedHashMap<>();
    }

    public PatternRankWithPie(int capacity) {
        super(capacity);
        this.pie = new ProbabilityPie(capacity);
        this.map = new LinkedHashMap<>(capacity);
    }

    @Override
    public AbstractProbabilityRank addMultiple(Collection<? extends Number> numbers) {
        this.maintain();

        for (Number number : numbers) {
            this.pie.add(number);
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
        return this;
    }

    @Override
    public AbstractProbabilityRank addMultiple(Map<? extends Number, Integer> map) {
        this.maintain();

        for (Map.Entry<? extends Number, Integer> entry : map.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                this.pie.add(entry.getKey());
                this.numbers.add(entry.getKey().doubleValue());
            }
        }
        this.sorted = false;
        return this;
    }

    @Override
    public AbstractProbabilityRank addMultiplePatterns(Collection<PatternValue> values) {
        this.maintain();

        for (PatternValue value : values) {
            this.pie.add(value.pattern);
            this.numbers.add(value.pattern.doubleValue());
        }
        this.sorted = false;
        return this;
    }

    @Override
    public AbstractProbabilityRank add(Number number) {
        this.maintain();
        this.pie.add(number);
        return super.add(number);
    }

    @Override
    public AbstractProbabilityRank remove(Number number) {
        this.pie.remove(number);
        return super.remove(number);
    }

    private void maintain() {
        if (!map.isEmpty()) {
            Iterator<Map.Entry<Long, Number>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Long, Number> entry = iterator.next();

                if (System.currentTimeMillis() - entry.getKey() > Check.violationCycleMilliseconds) {
                    iterator.remove();
                    this.remove(entry.getValue());
                } else {
                    break;
                }
            }
        }
    }
}
