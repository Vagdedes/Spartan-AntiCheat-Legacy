package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.AbstractPatternCache;

import java.util.Collection;

public class PatternAllCache extends AbstractPatternCache {

    Collection<Collection<Number>> data;

    PatternAllCache(Collection<Collection<Number>> cached) {
        super(true);
        this.data = cached;
    }

    Collection<Collection<Number>> update(Collection<Collection<Number>> cached) {
        this.data = cached;
        this.refresh();
        return this.data;
    }
}
