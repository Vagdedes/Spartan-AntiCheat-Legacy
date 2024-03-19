package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.AbstractPatternCache;

import java.util.Collection;

public class PatternAllCache extends AbstractPatternCache {

    Collection<Collection<PatternValue>> data;

    PatternAllCache(Collection<Collection<PatternValue>> cached) {
        super(true);
        this.data = cached;
    }

    Collection<Collection<PatternValue>> update(Collection<Collection<PatternValue>> cached) {
        this.data = cached;
        this.refresh();
        return this.data;
    }
}
