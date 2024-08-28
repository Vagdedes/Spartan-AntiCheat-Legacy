package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.PlayerDetectionSlots;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class Event_Status implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Join(PlayerJoinEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.createProtocol(n).spartanPlayer;

        if (Config.settings.getBoolean("Important.enable_watermark")
                && !Permissions.isStaff(p)) {
            p.sendMessage("");
            AwarenessNotifications.forcefullySend(
                    p,
                    "\nThis server is protected by the Spartan AntiCheat",
                    false
            );
            p.sendMessage("");
        }

        // Detections
        p.getExecutor(Enums.HackType.Speed).handle(false, e);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);

        SpartanBukkit.runDelayedTask(p, () -> {
            if (p != null) {
                Config.settings.runOnLogin(p);
                CloudBase.announce(p);
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Leave(PlayerQuitEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanProtocol protocol = SpartanBukkit.deleteProtocol(n);

        if (protocol == null) {
            return;
        }
        SpartanPlayer p = protocol.spartanPlayer;

        // Features
        PlayerDetectionSlots.remove(p);
        DetectionNotifications.runOnLeave(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Death(PlayerDeathEvent e) {
        Player n = e.getEntity();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

        // Detections
        p.getExecutor(Enums.HackType.AutoRespawn).run(false);
        p.getExecutor(Enums.HackType.ImpossibleInventory).handle(false, null);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);

        // Objects
        p.resetTrackers();
        p.movement.setDetectionLocation();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Respawn(PlayerRespawnEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

        // Objects
        p.resetTrackers();
        p.movement.setDetectionLocation();

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

}
