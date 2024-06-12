package com.vagdedes.spartan.listeners.protocol.async;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StratoCache {

    private static Map<Integer, LinkedList<Location>> cache = new ConcurrentHashMap<>();
    private static Map<Integer, Player> cachePlayer = new ConcurrentHashMap<>();
    private static Map<Integer, Long> lastPacket = new ConcurrentHashMap<>();
    public static Map<Integer, LinkedList<Location>> getCache() {
        return cache;
    }
    public static Player getPlayerByID(int id) {
        return (cachePlayer.getOrDefault(id, null));
    }
    public static void commitNewCache(int id) {
        cache.put(id, new LinkedList<>());
        pushNewPacket(id);
    }
    public static void pushNewMovement(int id, Player player, Location location) {
        cache.get(id).addFirst(location);
        cachePlayer.put(id, player);
        pushNewPacket(id);
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
        return lastPacket.containsKey(id) ? (int) ((System.currentTimeMillis() - lastPacket.get(id)) / 50) : 0;
    }

}
