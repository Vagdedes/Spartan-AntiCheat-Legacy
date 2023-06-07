package me.vagdedes.spartan.utils.data;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.utils.java.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownUtils {

    private static final Map<UUID, Map<String, Long>> hm = new ConcurrentHashMap<>(Config.getMaxPlayers());

    public static int get(UUID uuid, String name) {
        Map<String, Long> map = hm.get(uuid);

        if (map != null) {
            Long object = map.get(name);
            return object == null ? 0 : (int) Math.max(0, (object - System.currentTimeMillis()) / 50L);
        }
        return 0;
    }

    public static boolean canDo(UUID uuid, String name) {
        return get(uuid, name) == 0;
    }

    public static void add(UUID uuid, String name, int amount) {
        if (amount > 0) {
            Map<String, Long> map = hm.get(uuid);

            if (map == null) {
                map = new ConcurrentHashMap<>();
                hm.put(uuid, map);
            }
            map.put(name, System.currentTimeMillis() + (amount * 50L));
        }
    }

    public static void clear() {
        hm.clear();
    }

    public static void clear(String[] external) {
        int size = hm.size();

        if (size > 0) {
            for (Map.Entry<UUID, Map<String, Long>> entry : hm.entrySet()) {
                UUID uuid = entry.getKey();
                Set<String> keys = entry.getValue().keySet();
                int keysSize = keys.size();
                List<String> internal = new ArrayList<>(keysSize);

                for (String name : keys) {
                    if (!StringUtils.stringContainsPartOfArray(external, name)) {
                        internal.add(name);
                    }
                }

                if (keysSize == internal.size()) {
                    hm.remove(uuid);
                } else {
                    Map<String, Long> map = hm.get(uuid);

                    if (map != null) {
                        for (String name : internal) {
                            map.remove(name);
                        }
                    }
                }
                internal.clear();
            }
        }
    }

}
