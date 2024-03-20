package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.Pattern;

public abstract class PatternFamily {

    protected final long storeFrequency;
    protected long lastStored;
    protected final Pattern[] patterns;

    protected PatternFamily(long storeFrequency, int patterns) {
        this.storeFrequency = storeFrequency;
        this.lastStored = 0L;
        this.patterns = new Pattern[patterns];
    }

    protected void addPatterns(Pattern[] patterns) {
        for (int i = 0; i < patterns.length; i++) {
            this.patterns[i] = patterns[i];
        }
    }

    public void store() {
        if (System.currentTimeMillis() - this.lastStored >= this.storeFrequency) {
            this.lastStored = System.currentTimeMillis();

            for (Pattern patternValue : this.patterns) {
                patternValue.store();
            }
        }
    }
}
