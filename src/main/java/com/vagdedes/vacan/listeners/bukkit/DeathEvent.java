package com.vagdedes.vacan.listeners.bukkit;

import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.functionality.server.PluginBase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerDeathEvent e) {
        event(e.getEntity(), false, e);
    }

    public static void event(Player player, boolean packets, Object object) {
        PlayerProtocol protocol = PluginBase.getProtocol(player, true);
        protocol.timerBalancer.addBalance(50);

        if (protocol.packetsEnabled() == packets) {
            protocol.profile().executeRunners(null, object);
        }
    }

}
