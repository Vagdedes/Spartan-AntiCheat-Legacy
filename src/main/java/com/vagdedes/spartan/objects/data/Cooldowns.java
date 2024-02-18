package com.vagdedes.spartan.objects.data;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cooldowns { // todo synchronize

    private final Map<String, Long> storage;

    public Cooldowns(boolean async) {
        storage = (async && !MultiVersion.folia ? new ConcurrentHashMap<>() : new LinkedHashMap<>());
    }

    public void clear() {
        storage.clear();
    }

    public boolean has(String name) {
        return storage.containsKey(name);
    }

    public int get(String name) {
        Long object = storage.get(name);
        return object == null ? 0 :
                Math.max(AlgebraUtils.integerCeil((object - System.currentTimeMillis()) / TPS.tickTimeDecimal), 0);
    }

    public boolean canDo(String name) {
        return get(name) == 0;
    }

    public void add(String name, int amount) {
        if (amount > 0) {
            storage.put(name, System.currentTimeMillis() + (amount * 50L));
        } else {
            storage.remove(name);
        }
    }

    public void add(String[] names, int amount) {
        if (amount > 0) {
            for (String name : names) {
                storage.put(name, System.currentTimeMillis() + (amount * 50L));
            }
        } else {
            for (String name : names) {
                storage.remove(name);
            }
        }
    }

    public void remove(String name) {
        storage.remove(name);
    }

    public void remove(String[] names) {
        for (String name : names) {
            remove(name);
        }
    }

    public void clear(String s) {
        if (!storage.isEmpty()) {
            List<String> internal = new ArrayList<>();

            for (String name : storage.keySet()) {
                if (name.contains(s)) {
                    internal.add(name);
                }
            }
            remove(internal.toArray(new String[0]));
        }
    }
}
