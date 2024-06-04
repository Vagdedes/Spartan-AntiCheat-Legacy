package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.server.TPS;

import java.util.Map;

public class Cooldowns {

    private final Map<String, Long> storage;
    private final SpartanPlayer player;

    public Cooldowns(SpartanPlayer player, Map<String, Long> map) {
        this.storage = map;
        this.player = player;

        if (player == null) {
            Cache.store(this.storage);
        }
    }

    public int get(String name) {
        Long object = storage.get(name);

        if (object == null) {
            return 0;
        } else {
            if (player == null) {
                object -= System.currentTimeMillis();
            } else {
                object -= TPS.getTick(player);
            }
            return object < 0L ? 0 : object.intValue();
        }
    }

    public boolean canDo(String name) {
        return get(name) == 0;
    }

    public void add(String name, int ticks) {
        if (player == null) {
            storage.put(name, System.currentTimeMillis() + (ticks * TPS.tickTime));
        } else {
            storage.put(name, TPS.getTick(player) + ticks);
        }
    }

    public void add(String[] names, int ticks) {
        if (player == null) {
            for (String name : names) {
                storage.put(name, System.currentTimeMillis() + (ticks * TPS.tickTime));
            }
        } else {
            for (String name : names) {
                storage.put(name, TPS.getTick(player) + ticks);
            }
        }
    }

    public void remove(String name) {
        storage.remove(name);
    }

    public void remove(String[] names) {
        for (String name : names) {
            storage.remove(name);
        }
    }

}
