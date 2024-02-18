package com.vagdedes.spartan.objects.data;

import com.vagdedes.spartan.functionality.important.MultiVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Timer { // todo synchronize

    private final Map<String, Long> storage;

    public Timer(boolean async) {
        storage = (async && !MultiVersion.folia ? new ConcurrentHashMap<>() : new LinkedHashMap<>());
    }

    public void clear() {
        storage.clear();
    }

    public boolean has(long l) {
        return l != Long.MAX_VALUE;
    }

    public boolean has(String name) {
        return storage.get(name) != null;
    }

    public long get(String name, long def) {
        Long ms = storage.get(name);
        return ms != null ? System.currentTimeMillis() - ms : def;
    }

    public long get(String name) {
        return get(name, Long.MAX_VALUE);
    }

    public void set(String name) {
        storage.put(name, System.currentTimeMillis());
    }

    public void set(String name, long ms, boolean future) {
        storage.put(name, System.currentTimeMillis() + (future ? ms : -ms));
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
