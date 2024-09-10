package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.OverflowMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DataProcessor implements Listener {

    static {
        Register.enable(new DataProcessor());
    }

    static final Map<UUID, KeepSprint> keepSprint = new OverflowMap<>(new LinkedHashMap<>(), 1_024);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Death(EntityDeathEvent event) {
        keepSprint.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Leave(PlayerQuitEvent event) {
        keepSprint.remove(SpartanBukkit.getProtocol(event.getPlayer()).getUUID());
    }

    static KeepSprint getKeepSprint(UUID uuid) {
        return keepSprint.getOrDefault(
                uuid,
                new KeepSprint()
        );
    }
}
