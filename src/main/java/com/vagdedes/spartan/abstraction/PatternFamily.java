package com.vagdedes.spartan.abstraction;

import com.vagdedes.spartan.objects.statistics.PatternStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public abstract class PatternFamily {

    private static final Collection<PatternFamily> instances = Collections.synchronizedList(new ArrayList<>());

    public static void reload() {
        synchronized (instances) {
            for (PatternFamily instance : instances) {
                for (PatternStorage patternStorage : instance.patternValues) {
                    patternStorage.reload();
                }
            }
        }
    }

    // Separator

    protected final long storeFrequency;
    protected long lastStored;
    protected final PatternStorage[] patternValues;

    public PatternFamily(long storeFrequency, PatternStorage[] patternValues) {
        this.storeFrequency = storeFrequency;
        this.lastStored = 0L;
        this.patternValues = patternValues;

        synchronized (instances) {
            instances.add(this);
        }
    }

    public void store() {
        if (System.currentTimeMillis() - this.lastStored >= this.storeFrequency) {
            this.lastStored = System.currentTimeMillis();

            for (PatternStorage patternValue : this.patternValues) {
                patternValue.store();
            }
        }
    }
}
