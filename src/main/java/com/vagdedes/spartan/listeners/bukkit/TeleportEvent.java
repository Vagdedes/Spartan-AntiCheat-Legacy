package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.check.implementation.movement.morepackets.MorePackets;
import com.vagdedes.spartan.abstraction.protocol.PlayerBukkit;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.PluginBase;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Teleport(PlayerTeleportEvent e) {
        teleport(e.getPlayer(), false);
    }

    public static void teleport(Player player, boolean packets) {
        PlayerProtocol protocol = PluginBase.getProtocol(player, true);

        if (protocol.packetsEnabled() == packets) {
            PlayerBukkit p = protocol.bukkitExtra;

            // Object
            p.resetCrucialData();

            // Detections
            protocol.profile().getRunner(Enums.HackType.MorePackets).handle(false, MorePackets.TELEPORT);
            protocol.profile().getRunner(Enums.HackType.IrregularMovements).handle(false, null);
            p.trackers.add(PlayerTrackers.TrackerType.TELEPORT, "tp", 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Respawn(PlayerRespawnEvent e) {
        respawn(e.getPlayer(), false);
    }

    public static void respawn(Player player, boolean packets) {
        PlayerProtocol protocol = PluginBase.getProtocol(player, true);

        if (protocol.packetsEnabled() == packets) {
            // Objects
            protocol.bukkitExtra.resetCrucialData();
        }
    }

}
