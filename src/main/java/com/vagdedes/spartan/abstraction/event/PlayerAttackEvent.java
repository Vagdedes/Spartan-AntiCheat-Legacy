package com.vagdedes.spartan.abstraction.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PlayerAttackEvent {
    private final Player player;
    private final Entity target;
    private final boolean cancelled;

    public PlayerAttackEvent(Player player, Entity target, boolean cancelled) {
        this.player = player;
        this.target = target;
        this.cancelled = cancelled;
    }

    public Entity getTarget() {
        return target;
    }
    public boolean isCancelled() {
        return cancelled;
    }

    public Player getPlayer() {
        return player;
    }
}
