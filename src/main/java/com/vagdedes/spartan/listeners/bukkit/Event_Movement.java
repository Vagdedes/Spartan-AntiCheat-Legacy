package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.protocol.SuperPositionPacket;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class Event_Movement implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void MoveEvent(PlayerMoveEvent e) {
        event(e);
        SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer.getExecutor(
                Enums.HackType.MorePackets
        ).handle(e.isCancelled(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void WorldEvent(PlayerChangedWorldEvent e) {
        SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer.resetData(true);
    }

    public static void event(PlayerMoveEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;
        Location nto = e.getTo();

        if (nto == null) {
            p.movement.judgeGround();
            return;
        }
        SpartanLocation vehicle = p.movement.getVehicleLocation();
        SpartanLocation
                to = vehicle != null ? vehicle : new SpartanLocation(nto),
                from = new SpartanLocation(e.getFrom());

        // Values
        double xDiff = to.getX() - from.getX(),
                zDiff = to.getZ() - from.getZ(),
                preXZ = (xDiff * xDiff) + (zDiff * zDiff),
                toY = to.getY(),
                fromY = from.getY(),
                dis = Math.sqrt(preXZ + AlgebraUtils.getSquare(toY, fromY)),
                box = toY - to.getBlockY(),
                ver = toY - fromY,
                hor = Math.sqrt(preXZ);

        if (!p.movement.processLastMoveEvent(
                nto,
                vehicle,
                to,
                from,
                dis,
                hor,
                ver,
                xDiff,
                zDiff,
                box
        )) {
            p.movement.judgeGround();
            return;
        }
        MovementProcessing.run(p, to, ver, box);

        // Detections
        boolean cancelled = e.isCancelled();
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, null);
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, e);
        p.getExecutor(Enums.HackType.ImpossibleInventory).run(cancelled);
        p.getExecutor(Enums.HackType.IrregularMovements).run(cancelled);
        p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
        p.getExecutor(Enums.HackType.KillAura).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Criticals).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Simulation).handle(cancelled, e);
    }

    public static void superPosition(SuperPositionPacket packet) {
        SpartanPlayer p = SpartanBukkit.getProtocol(packet.getProtocol().player).spartanPlayer;
        boolean cancelled = packet.getEvent().isCancelled();
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, packet);
    }

}
