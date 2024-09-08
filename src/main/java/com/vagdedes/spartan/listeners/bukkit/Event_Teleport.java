package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Event_Teleport implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void teleport(PlayerTeleportEvent e) { // todo call in protocol
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Object
        p.resetData(false);
        p.movement.judgeGround();

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void death(PlayerDeathEvent e) { // todo call in protocol
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getEntity()).spartanPlayer;

        // Detections
        p.getExecutor(Enums.HackType.AutoRespawn).handle(false, null);
        p.getExecutor(Enums.HackType.ImpossibleInventory).handle(false, null);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);

        // Objects
        p.resetData(true);
        p.movement.setDetectionLocation();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void respawn(PlayerRespawnEvent e) { // todo call in protocol
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Objects
        p.resetData(true);
        p.movement.setDetectionLocation();

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

}
