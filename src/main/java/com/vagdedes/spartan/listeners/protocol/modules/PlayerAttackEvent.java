package com.vagdedes.spartan.listeners.protocol.modules;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PlayerAttackEvent {
    private final Player player;
    private final Entity target;

    public PlayerAttackEvent(Player player, Entity target) {
        this.player = player;
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }

    public Player getPlayer() {
        return player;
    }
}
