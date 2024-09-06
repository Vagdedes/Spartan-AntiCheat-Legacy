package com.vagdedes.spartan.listeners.bukkit.protocol.replicated;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Event_Teleport implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Teleport(PlayerTeleportEvent e) {
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Object
        p.resetTrackers();
        p.movement.judgeGround();

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Death(PlayerDeathEvent e) {
        Player n = e.getEntity();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanPlayer p = SpartanBukkit.getProtocol(n).spartanPlayer;

        // Detections
        p.getExecutor(Enums.HackType.AutoRespawn).handle(false, null);
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
