package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
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
        teleport(e, false);
    }

    public static void teleport(PlayerTeleportEvent e, boolean packets) { // todo call in protocol
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            SpartanPlayer p = protocol.spartanPlayer;
            // Object
            p.resetData(false);
            p.movement.judgeGround();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Death(PlayerDeathEvent e) {
        death(e.getEntity(), false);
    }

    public static void death(Player player, boolean packets) { // todo call in protocol
        SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

        if (protocol.packetsEnabled() == packets) {
            SpartanPlayer p = protocol.spartanPlayer;

            // Detections
            p.getExecutor(Enums.HackType.AutoRespawn).handle(false, null);
            p.getExecutor(Enums.HackType.ImpossibleInventory).handle(false, null);
            // Objects
            p.resetData(true);
            p.movement.setDetectionLocation();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Respawn(PlayerRespawnEvent e) {
        respawn(e, false);
    }

    public static void respawn(PlayerRespawnEvent e, boolean packets) { // todo call in protocol
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            SpartanPlayer p = protocol.spartanPlayer;

            // Objects
            p.resetData(true);
            p.movement.setDetectionLocation();
        }
    }

}
