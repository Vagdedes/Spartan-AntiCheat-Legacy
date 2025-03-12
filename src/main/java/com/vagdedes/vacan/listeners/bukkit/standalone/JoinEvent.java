package com.vagdedes.vacan.listeners.bukkit.standalone;

import com.vagdedes.vacan.Register;
import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.functionality.connection.CloudBase;
import com.vagdedes.vacan.functionality.moderation.AwarenessNotifications;
import com.vagdedes.vacan.functionality.server.Config;
import com.vagdedes.vacan.functionality.server.Permissions;
import com.vagdedes.vacan.functionality.server.PluginBase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerJoinEvent e) {
        Player n = e.getPlayer();
        PlayerProtocol protocol = PluginBase.getProtocol(n);

        if (Config.settings.getBoolean("Important.enable_watermark")
                && !Permissions.isStaff(n)) {
            n.sendMessage("");
            AwarenessNotifications.forcefullySend(
                    protocol,
                    "\nThis server is protected by the " + Register.pluginName + " AntiCheat",
                    false
            );
            n.sendMessage("");
        }

        PluginBase.runDelayedTask(protocol, () -> {
            Config.settings.runOnLogin(protocol);
            CloudBase.announce(protocol);
        }, 10L);
    }

}
