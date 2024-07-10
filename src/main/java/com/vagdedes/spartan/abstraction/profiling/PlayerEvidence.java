package com.vagdedes.spartan.abstraction.profiling;

import me.vagdedes.spartan.system.Enums;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEvidence {

    private final Map<Enums.HackType, String> historical, informational; // Live object is used for synchronization

    PlayerEvidence() {
        this.historical = new ConcurrentHashMap<>(2, 1.0f);
        this.informational = new ConcurrentHashMap<>(2, 1.0f);
    }

    // Separator

    public void clear(boolean historical, boolean informational) {
        if (historical) {
            this.historical.clear();
        }
        if (informational) {
            this.informational.clear();
        }
    }

    public boolean has(Enums.HackType hackType) {
        return this.historical.containsKey(hackType);
    }

    public void remove(Enums.HackType hackType, boolean historical, boolean informational) {
        if (historical) {
            this.historical.remove(hackType);
        }
        if (informational) {
            this.informational.remove(hackType);
        }
    }

    public void add(Enums.HackType hackType, String info,
                    boolean historical,
                    boolean informational) {
        if (historical) {
            this.historical.put(hackType, info);
        }
        if (informational) {
            this.informational.put(hackType, info);
        }
    }

    // Separator

    public Collection<Enums.HackType> getKnowledgeList(boolean informational) {
        Collection<Enums.HackType> set = new HashSet<>(this.historical.keySet());

        if (informational) {
            set.addAll(this.informational.keySet());
        }
        return set;
    }

    public Set<Map.Entry<Enums.HackType, String>> getKnowledgeEntries(boolean informational) {
        Set<Map.Entry<Enums.HackType, String>> set = new HashSet<>(this.historical.entrySet());

        if (informational) {
            set.addAll(this.informational.entrySet());
        }
        return set;
    }

}
