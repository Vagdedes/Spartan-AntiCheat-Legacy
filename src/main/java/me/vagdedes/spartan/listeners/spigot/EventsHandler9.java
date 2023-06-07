package me.vagdedes.spartan.listeners.spigot;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.protections.PlayerLimitPerIP;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudConnections;
import me.vagdedes.spartan.handlers.stability.Chunks;
import me.vagdedes.spartan.system.IDs;
import me.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.net.InetAddress;

public class EventsHandler9 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void ChunkLoad(ChunkLoadEvent e) {
        Chunks.load(e.getWorld(), e.getChunk());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void ChunkUnload(ChunkUnloadEvent e) {
        Chunks.unload(e.getWorld(), e.getChunk());
    }

    @EventHandler
    private void PreLoginEvent(AsyncPlayerPreLoginEvent e) {
        // Features
        if (IDs.isValid() && e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            InetAddress address = e.getAddress();
            String ipAddress = address != null ? PlayerLimitPerIP.get(address) : null;
            CloudConnections.updatePunishedPlayer(e.getUniqueId(), ipAddress);
        }
    }

    @EventHandler
    private void PluginEnable(PluginEnableEvent e) {

        // Utils
        PluginUtils.clear();

        // System
        Compatibility.memoryRefresh();
    }

    @EventHandler
    private void PluginDisable(PluginDisableEvent e) {

        // Utils
        PluginUtils.clear();

        // System
        Compatibility.memoryRefresh();
    }
}
