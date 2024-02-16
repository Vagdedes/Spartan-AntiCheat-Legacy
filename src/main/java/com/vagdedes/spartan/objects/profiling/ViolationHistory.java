package com.vagdedes.spartan.objects.profiling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ViolationHistory {

    private final Collection<PlayerViolation> memory;
    private final boolean sync;

    public ViolationHistory(Collection<PlayerViolation> list, boolean sync) { // Constructor
        this.memory = sync ? Collections.synchronizedCollection(list) : list;
        this.sync = sync;
    }

    ViolationHistory() { // Local
        this(new ArrayList<>(), true);
    }

    public void clear() {
        if (sync) {
            synchronized (memory) {
                memory.clear();
            }
        } else {
            memory.clear();
        }
    }

    public void store(PlayerViolation playerViolation) {
        if (sync) {
            synchronized (memory) {
                memory.add(playerViolation);
            }
        } else {
            memory.add(playerViolation);
        }
    }

    public int getCount() {
        return memory.size();
    }

    public Collection<PlayerViolation> getMemory() {
        if (sync) {
            synchronized (memory) {
                return new ArrayList<>(memory);
            }
        } else {
            return new ArrayList<>(memory);
        }
    }

}
