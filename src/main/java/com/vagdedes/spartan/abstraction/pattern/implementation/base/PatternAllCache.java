package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.AbstractPatternCache;

import java.util.Collection;

public class PatternAllCache extends AbstractPatternCache {

    Collection<Collection<Double>> data;

    PatternAllCache(Collection<Collection<Double>> cached) {
        super(true);
        this.data = cached;
    }

    Collection<Collection<Double>> update(Collection<Collection<Double>> cached) {
        this.data = cached;
        this.refresh();
        return this.data;
    }
}
