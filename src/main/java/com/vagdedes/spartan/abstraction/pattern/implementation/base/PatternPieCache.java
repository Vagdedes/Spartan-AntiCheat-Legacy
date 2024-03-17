package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternCache;
import com.vagdedes.spartan.utils.math.probability.ProbabilityPie;

public class PatternPieCache extends PatternCache {

    ProbabilityPie data;

    PatternPieCache(ProbabilityPie cached) {
        super(true);
        this.data = cached;
    }
}
