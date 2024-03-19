package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.math.implementation.ProbabilityPie;
import com.vagdedes.spartan.abstraction.pattern.AbstractPatternCache;

public class PatternPieCache extends AbstractPatternCache {

    ProbabilityPie data;

    PatternPieCache(ProbabilityPie cached) {
        super(true);
        this.data = cached;
    }
}
