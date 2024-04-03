package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.AbstractPatternCache;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

public class PatternRankCache extends AbstractPatternCache {

    ProbabilityPie data;

    PatternRankCache(ProbabilityPie cached) {
        super(true);
        this.data = cached;
    }

    ProbabilityPie update(ProbabilityPie cached) {
        this.data = cached;
        this.refresh();
        return this.data;
    }
}
