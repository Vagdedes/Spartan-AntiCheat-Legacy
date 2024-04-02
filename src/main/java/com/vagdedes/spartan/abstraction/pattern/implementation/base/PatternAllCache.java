package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.AbstractPatternCache;

import java.util.Collection;

public class PatternAllCache extends AbstractPatternCache {

    Collection<Collection<Float>> data;

    PatternAllCache(Collection<Collection<Float>> cached) {
        super(true);
        this.data = cached;
    }

    Collection<Collection<Float>> update(Collection<Collection<Float>> cached) {
        this.data = cached;
        this.refresh();
        return this.data;
    }
}
