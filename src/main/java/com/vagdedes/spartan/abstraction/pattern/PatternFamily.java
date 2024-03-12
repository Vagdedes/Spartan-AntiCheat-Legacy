package com.vagdedes.spartan.abstraction.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public abstract class PatternFamily {

    private static final Collection<PatternFamily> instances = Collections.synchronizedList(new ArrayList<>());

    public static void reload() {
        synchronized (instances) {
            for (PatternFamily instance : instances) {
                for (PatternStorage patternStorage : instance.patterns) {
                    patternStorage.reload();
                }
            }
        }
    }

    // Separator

    protected final long storeFrequency;
    protected long lastStored;
    protected final PatternStorage[] patterns;

    public PatternFamily(long storeFrequency, int patterns) {
        this.storeFrequency = storeFrequency;
        this.lastStored = 0L;
        this.patterns = new PatternStorage[patterns];

        synchronized (instances) {
            instances.add(this);
        }
    }

    protected void addPatterns(PatternStorage[] patterns) {
        for (int i = 0; i < patterns.length; i++) {
            this.patterns[i] = patterns[i];
        }
    }

    public void store() {
        if (System.currentTimeMillis() - this.lastStored >= this.storeFrequency) {
            this.lastStored = System.currentTimeMillis();

            for (PatternStorage patternValue : this.patterns) {
                patternValue.store();
            }
        }
    }
}
