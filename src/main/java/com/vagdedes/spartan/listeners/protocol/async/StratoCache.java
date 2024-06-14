package com.vagdedes.spartan.listeners.protocol.async;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StratoCache {

    private StratoCache() {
        // stub!
    }

    private static final Map<Integer, LinkedList<Location>> cache = new ConcurrentHashMap<>();
    private static final Map<Integer, Player> cachePlayer = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> lastPacket = new ConcurrentHashMap<>();
    private static final Map<Integer, LinkedList<Integer>> delays = new ConcurrentHashMap<>();
    public static Map<Integer, LinkedList<Location>> getCache() {
        return cache;
    }
    public static Player getPlayerByID(int id) {
        return (cachePlayer.getOrDefault(id, null));
    }
    public static void commitNewCache(int id) {
        cache.put(id, new LinkedList<>());
        lastPacket.put(id, System.currentTimeMillis());
        delays.put(id, new LinkedList<>());
        pushNewPacket(id);
    }
    public static void pushNewMovement(int id, Player player, Location location) {
        cache.getOrDefault(id, new LinkedList<>()).addFirst(location);
        cachePlayer.put(id, player);
        delays.getOrDefault(id, new LinkedList<>()).add((int) (System.currentTimeMillis()
                                - lastPacket.getOrDefault(id, System.currentTimeMillis())));
        lastPacket.put(id, System.currentTimeMillis());
        pushNewPacket(id);
        if (delays.getOrDefault(id, new LinkedList<>()).size() >= 10) {
            LinkedList<Integer> l = new LinkedList<>();
            l.add(Collections.max(delays.get(id)) / 2);
            delays.put(id, l);
        }
    }
    public static void pushNewPacket(int id) {
        lastPacket.put(id, System.currentTimeMillis());
    }
    public static Location getLocationWithTickRollback(int id, int ticks) {
        if (!cache.containsKey(id)) return null;
        LinkedList<Location> locationStack = cache.get(id);
        if (locationStack.isEmpty()) return null;
        return locationStack.get((ticks >= locationStack.size()) ? locationStack.size() - 1 : ticks);
    }
    public static int getPlayerTicksDelay(int id) {
        return delays.containsKey(id) ? Collections.max(delays.get(id)) / 50 : 0;
    }

}
