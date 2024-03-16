package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternCache;

import java.util.ArrayList;
import java.util.Collection;

public class PatternAllCache extends PatternCache {

    private Collection<Collection<PatternValue>> data;

    PatternAllCache(Collection<Collection<PatternValue>> cached) {
        super();
        this.data = cached;
    }

    Collection<Collection<PatternValue>> update(Collection<Collection<PatternValue>> cached) {
        this.data = cached;
        this.refresh();
        return this.canShow() ? this.data : new ArrayList<>(0);
    }

    Collection<Collection<PatternValue>> get() {
        return this.canShow() ? this.data : new ArrayList<>(0);
    }
}
