package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.AbstractPatternCache;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

public class PatternPieCache extends AbstractPatternCache {

    ProbabilityPie data;

    PatternPieCache(ProbabilityPie cached) {
        super(false);
        this.data = cached;
    }
}
