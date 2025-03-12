package com.vagdedes.vacan.listeners.bukkit.standalone;

import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.functionality.server.PluginBase;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(AsyncPlayerChatEvent e) {
        PlayerProtocol p = PluginBase.getProtocol(e.getPlayer());
        p.profile().executeRunners(e.isCancelled(), e);
    }

}
