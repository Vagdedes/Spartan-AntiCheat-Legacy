package com.vagdedes.spartan.listeners.protocol;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanLocation;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.player.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.listeners.protocol.modules.PlayerAttackEvent;
import com.vagdedes.spartan.listeners.protocol.modules.PlayerStayEvent;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Shared {

    public static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            Enums.HackType.NoFall,
            Enums.HackType.IrregularMovements,
            Enums.HackType.Speed,
            Enums.HackType.MorePackets,
            Enums.HackType.ImpossibleInventory,
            Enums.HackType.Exploits
    };

    public static void join(PlayerJoinEvent e) {
        Player n = e.getPlayer();

        // Utils
        if (PlayerLimitPerIP.add(n)) {
            e.setJoinMessage(null);
            return;
        }
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        // System
        MaximumCheckedPlayers.add(p);

        if (!SpartanEdition.attemptNotification(p)
                && Config.settings.getBoolean("Important.enable_watermark")) {
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

        SpartanBukkit.runDelayedTask(p, () -> {
            if (p != null) {
                Config.settings.runOnLogin(p);
                CloudBase.announce(p);
            }
        }, 10);

        // Trackers
        SpartanBukkit.getProtocol(n).canCheck = true;
    }

    public static void move(PlayerMoveEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);
        if (p == null) {
            return;
        }
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanLocation vehicle = p.movement.getVehicleLocation(n);
        SpartanLocation
                to = vehicle != null ? vehicle : new SpartanLocation(nto),
                from = new SpartanLocation(e.getFrom());
        from.retrieveDataFrom(to);

        // Values
        double preXZ = AlgebraUtils.getSquare(to.getX(), from.getX()) + AlgebraUtils.getSquare(to.getZ(), from.getZ()),
                toY = to.getY(),
                fromY = from.getY(),
                dis = Math.sqrt(preXZ + AlgebraUtils.getSquare(toY, fromY)),
                box = toY - to.getBlockY(),
                ver = toY - fromY,
                hor = Math.sqrt(preXZ);

        if (!p.movement.processLastMoveEvent(to, from, dis, hor, ver, box)) {
            return;
        }
        MovementProcessing.run(p, to, ver, box);

        // Patterns
        Bukkit.getScheduler().runTask(Register.plugin, () -> {
            for (Enums.HackType hackType : handledChecks) {
                if (p.getViolations(hackType).prevent()) {
                    break;
                }
            }
        });

        // Detections
        boolean cancelled = e.isCancelled();
        p.getExecutor(Enums.HackType.Exploits).handle(cancelled, null);
        p.getExecutor(Enums.HackType.ImpossibleInventory).run(cancelled);
        p.getExecutor(Enums.HackType.KillAura).run(cancelled);
        p.getExecutor(Enums.HackType.NoFall).run(cancelled);
        p.getExecutor(Enums.HackType.IrregularMovements).run(cancelled);
        p.getExecutor(Enums.HackType.Speed).run(cancelled);
        p.getExecutor(Enums.HackType.MorePackets).run(cancelled);
    }

    public static void teleport(PlayerTeleportEvent e) {
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        SpartanLocation to = new SpartanLocation(nto);

        // Object
        p.movement.judgeGround();

        // Trackers
        SpartanProtocol protocol = SpartanBukkit.getProtocol(n);
        protocol.position = nto;
        protocol.lastRotation.setYaw(to.getYaw());
        protocol.lastRotation.setPitch(to.getPitch());
        protocol.lastTeleport = nto;

        // Detections
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

    public static void velocity(PlayerVelocityEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // Object
        p.addReceivedVelocity(e);

        // Detections
        p.getExecutor(Enums.HackType.Speed).handle(e.isCancelled(), e);

        if (p.getViolations(Enums.HackType.Velocity).prevent()) {
            e.setCancelled(true);
        }
    }

    public static void stay(PlayerStayEvent e) {
        // ONLY WORK WITH PACKETS!!! ! !!! !
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }

        // Detections
        p.getExecutor(Enums.HackType.Speed).handle(false, e);
    }
    public static void attack(PlayerAttackEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }

        double[] utility = CombatUtils.get_X_Y_Distance(p, (LivingEntity) e.getTarget());

        boolean cancelled = e.isCancelled();
        // Detections
        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
        p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Criticals).handle(cancelled, e);

        if (utility != null) {
            Object[] objects = new Object[]{e.getTarget(), utility};
            p.getExecutor(Enums.HackType.KillAura).run(cancelled);
            p.getExecutor(Enums.HackType.KillAura).handle(cancelled, objects);
            p.getExecutor(Enums.HackType.HitReach).handle(cancelled, objects);
        }

    }

}
