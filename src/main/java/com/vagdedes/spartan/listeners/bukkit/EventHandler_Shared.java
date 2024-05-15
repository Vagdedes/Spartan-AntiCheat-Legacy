package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.listeners.omnibridge.MovementEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class EventHandler_Shared implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Move(PlayerMoveEvent e) {
        MovementEvents.move(e);
    }

}
