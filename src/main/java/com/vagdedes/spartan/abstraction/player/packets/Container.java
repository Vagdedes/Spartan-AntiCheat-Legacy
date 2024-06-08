package com.vagdedes.spartan.abstraction.player.packets;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Container {
    private static final Map<Player, IPlayer> playerContainer = new ConcurrentHashMap<>();
    public static void pushNewPlayer(Player player) {
        playerContainer.put(player, new IPlayer(player));
    }
    public static IPlayer getIPlayer(Player player) {
        return playerContainer.getOrDefault(player, new IPlayer(player));
    }
}
