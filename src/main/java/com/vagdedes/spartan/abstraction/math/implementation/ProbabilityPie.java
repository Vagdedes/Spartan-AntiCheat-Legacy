package com.vagdedes.spartan.abstraction.math.implementation;

import com.vagdedes.spartan.abstraction.math.AbstractProbabilityPie;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternValue;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ProbabilityPie extends AbstractProbabilityPie {

    public ProbabilityPie() {
        super(-1);
    }

    public ProbabilityPie(int capacity) {
        super(capacity);
    }
}
