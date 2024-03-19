package com.vagdedes.spartan.abstraction.pattern;

public abstract class AbstractPatternCache {

    private long time;

    protected AbstractPatternCache(boolean refresh) {
        if (refresh) {
            refresh();
        } else {
            this.time = System.currentTimeMillis();
        }
    }

    public void refresh() {
        this.time = System.currentTimeMillis() + 1_000L;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > this.time;
    }
}
