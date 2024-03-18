package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternCache;
import com.vagdedes.spartan.abstraction.math.implementation.ProbabilityRank;

public class PatternRankCache extends PatternCache {

    ProbabilityRank data;

    PatternRankCache(ProbabilityRank cached) {
        super(true);
        this.data = cached;
    }

    ProbabilityRank update(ProbabilityRank cached) {
        this.data = cached;
        this.refresh();
        return this.data;
    }
}
