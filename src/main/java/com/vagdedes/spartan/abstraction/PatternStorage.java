package com.vagdedes.spartan.abstraction;

import java.util.Map;
import java.util.UUID;

public abstract class PatternStorage {

    protected final long storeFrequency;
    protected final long lastStored;

    public PatternStorage(long storeFrequency) {
        this.storeFrequency = storeFrequency;
        this.lastStored = 0L;
    }
}
