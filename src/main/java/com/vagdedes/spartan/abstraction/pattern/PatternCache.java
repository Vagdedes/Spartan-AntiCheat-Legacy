package com.vagdedes.spartan.abstraction.pattern;

public abstract class PatternCache {

    private long time;
    private int count;

    protected PatternCache() {
        this.count = 0;
        refresh();
    }

    protected void refresh() {
        this.time = System.currentTimeMillis() + 1_000L;
        this.count++;
    }

    public boolean isExpired() {
        if (System.currentTimeMillis() > this.time) {
            return true;
        } else {
            this.count++;
            return false;
        }
    }

    protected boolean canShow() {
        return this.count >= 50;
    }
}
