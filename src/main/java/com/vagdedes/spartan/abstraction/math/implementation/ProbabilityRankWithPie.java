package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractProbabilityRank;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternValue;
import com.vagdedes.spartan.utils.math.probability.ProbabilityPie;

import java.util.Collection;
import java.util.Map;

public class ProbabilityRankWithPie extends AbstractProbabilityRank {

    public final ProbabilityPie pie;

    public ProbabilityRankWithPie() {
        super();
        this.pie = new ProbabilityPie();
    }

    public ProbabilityRankWithPie(int capacity) {
        super(capacity);
        this.pie = new ProbabilityPie(capacity);
    }

    @Override
    public AbstractProbabilityRank addMultiple(Collection<? extends Number> numbers) {
        for (Number number : numbers) {
            this.pie.add(number);
            this.numbers.add(number.doubleValue());
        }
        this.sorted = false;
        return this;
    }

    @Override
    public AbstractProbabilityRank addMultiple(Map<? extends Number, Integer> map) {
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
        for (PatternValue value : values) {
            this.pie.add(value.pattern);
            this.numbers.add(value.pattern.doubleValue());
        }
        this.sorted = false;
        return this;
    }

    @Override
    public AbstractProbabilityRank add(Number number) {
        this.pie.add(number);
        return super.add(number);
    }
}
