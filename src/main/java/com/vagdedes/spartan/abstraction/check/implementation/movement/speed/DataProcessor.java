package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

import com.vagdedes.spartan.Register;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataProcessor implements Listener {

    static {
        Register.enable(new DataProcessor(), DataProcessor.class);
    }

    static final Map<UUID, KeepSprint> keepSprint = new ConcurrentHashMap<>();
    static final Map<UUID, Entity> target = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Death(EntityDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        keepSprint.remove(uuid);
        target.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Leave(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        keepSprint.remove(uuid);
        target.remove(uuid);
    }

}
