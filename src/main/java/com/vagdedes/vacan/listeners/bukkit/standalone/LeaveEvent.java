package com.vagdedes.vacan.listeners.bukkit.standalone;

import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.functionality.moderation.DetectionNotifications;
import com.vagdedes.vacan.functionality.server.PluginBase;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LeaveEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerQuitEvent e) {
        PlayerProtocol protocol = PluginBase.deleteProtocol(e.getPlayer());

        if (protocol == null) {
            return;
        }
        DetectionNotifications.runOnLeave(protocol);
    }

}
