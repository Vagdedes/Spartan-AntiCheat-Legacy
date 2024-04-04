package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.AbstractPatternCache;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

public class PatternNonLinearCache extends AbstractPatternCache {

    ProbabilityPie data;

    PatternNonLinearCache(ProbabilityPie cached) {
        super(true);
        this.data = cached;
    }

    ProbabilityPie update(ProbabilityPie cached) {
        this.data = cached;
        this.refresh();
        return this.data;
    }
}
