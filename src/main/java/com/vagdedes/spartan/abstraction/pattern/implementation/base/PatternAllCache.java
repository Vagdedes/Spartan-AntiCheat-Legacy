package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternCache;

import java.util.Collection;

public class PatternAllCache extends PatternCache {

    Collection<Collection<PatternValue>> data;

    PatternAllCache(Collection<Collection<PatternValue>> cached) {
        super();
        this.data = cached;
    }

    void update(Collection<Collection<PatternValue>> cached) {
        this.data = cached;
        this.refresh();
    }
}
