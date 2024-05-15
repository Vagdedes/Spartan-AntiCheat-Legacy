package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.listeners.SharedEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class EventHandler_Shared implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Move(PlayerMoveEvent e) {
        SharedEvents.move(e);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void Teleport(PlayerTeleportEvent e) {
        SharedEvents.teleport(e);
    }

}
