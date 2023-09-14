package me.vagdedes.spartan.interfaces.listeners;

import me.vagdedes.spartan.checks.implementation.combat.VelocityCheck;
import me.vagdedes.spartan.checks.implementation.combat.killAura.KillAura;
import me.vagdedes.spartan.checks.implementation.exploits.ChunkUpdates;
import me.vagdedes.spartan.checks.implementation.exploits.HeadPosition;
import me.vagdedes.spartan.checks.implementation.exploits.UndetectedMovement;
import me.vagdedes.spartan.checks.implementation.inventory.ImpossibleInventory;
import me.vagdedes.spartan.checks.implementation.movement.EntityMove;
import me.vagdedes.spartan.checks.implementation.movement.MorePackets;
import me.vagdedes.spartan.checks.implementation.movement.NoFall;
import me.vagdedes.spartan.checks.implementation.movement.NoSlowdown;
import me.vagdedes.spartan.checks.implementation.movement.irregularmovements.IrregularMovements;
import me.vagdedes.spartan.checks.implementation.movement.speed.Speed;
import me.vagdedes.spartan.checks.implementation.movement.speed.SprintSpeed;
import me.vagdedes.spartan.checks.implementation.movement.speed.WaterSpeed;
import me.vagdedes.spartan.features.chat.ChatProtection;
import me.vagdedes.spartan.features.moderation.Debug;
import me.vagdedes.spartan.features.protections.ServerFlying;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.handlers.tracking.MovementProcessing;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.profiling.PlayerFight;
import me.vagdedes.spartan.objects.profiling.PlayerOpponent;
import me.vagdedes.spartan.objects.profiling.PlayerVelocity;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class EventsHandler7 implements Listener {

    private static final long damageTime = PlayerVelocity.maximumCollection * 50L;
    private static boolean heavyMovementChecks = false;
    public static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            NoFall.check,
            IrregularMovements.check,
            NoSlowdown.check,
            EntityMove.check,
            Speed.check,
            MorePackets.check,
            ImpossibleInventory.check,
            Enums.HackType.Exploits
    };

    static {
        refresh();
    }

    public static void refresh() {
        heavyMovementChecks = false;

        for (Enums.HackType hackType :
                new Enums.HackType[]{
                        NoFall.check,
                        IrregularMovements.check,
                        NoSlowdown.check,
                        EntityMove.check,
                        Speed.check,
                        MorePackets.check}) {
            if (hackType.getCheck().isEnabled(null, null, null)) {
                heavyMovementChecks = true;
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void PlayerFlight(PlayerToggleFlightEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }

        // Objects
        p.setFlying(e.isCancelled() ? n.isFlying() : e.isFlying() || n.isFlying(), n.getAllowFlight());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void TabCompletion(PlayerChatTabCompleteEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // Protections
        if (ChatProtection.runCommand(p, e.getChatMessage(), true)) {
            e.getTabCompletions().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Command(ServerCommandEvent e) {
        CommandSender s = e.getSender();
        String msg = e.getCommand();

        // Protections
        if (ChatProtection.runConsoleCommand(s, msg)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Move(PlayerMoveEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        PlayerData.update(p, n, false);
        Decimals decimals = p.getDecimals();

        if (e.isCancelled()) {
            return;
        }
        if (!p.canDo(false)) {
            ServerFlying.run(p);
            return;
        }
        Location nto = e.getTo();

        if (nto == null) {
            return;
        }
        SpartanLocation to = p.setEventLocation(nto),
                from = new SpartanLocation(p, e.getFrom());
        p.processLastMoveEvent(to, from);

        // Objects
        to.retrieveDataFrom(p.getLocation());
        from.retrieveDataFrom(to);

        if (to.getYaw() != from.getYaw() || to.getPitch() != from.getPitch()) {
            p.setLastHeadMovement();
        }

        // Handlers
        for (Enums.HackType hackType : handledChecks) {
            if (p.getViolations(hackType).process()) {
                return;
            }
        }

        // Values
        double preXZ = AlgebraUtils.getPreDistance(to.getX(), from.getX()) + AlgebraUtils.getPreDistance(to.getZ(), from.getZ()),
                toY = to.getY(),
                fromY = from.getY(),
                dis = Math.sqrt(preXZ + AlgebraUtils.getPreDistance(toY, fromY)),
                rem = toY - to.getBlockY(),
                ver = toY - fromY,
                hor = Math.sqrt(preXZ);
        Buffer buffer = p.getBuffer();
        int tick = buffer.start("move-event", 1);
        boolean crawling = p.isCrawling(),
                firstTick = tick == 1,
                longDistance = dis >= MoveUtils.nearMaxFallingMotion,
                repetitionDistance = firstTick || longDistance,
                repetition = repetitionDistance || p.getProfile().isSuspectedOrHacker(handledChecks),
                checkPlayer = longDistance || !p.canSkipDetectionTick(SpartanLocation.clearanceTick);

        // Handlers
        Cooldowns cooldowns = p.getCooldowns();
        CombatProcessing.runMove(p, to, cooldowns, decimals);

        // Objects
        PlayerFight playerFight = p.getProfile().getCombat().getCurrentFightByCache();

        if (playerFight != null) {
            PlayerOpponent[] playerOpponents = playerFight.getOpponent(p);
            PlayerOpponent self = playerOpponents[0];

            if (self.getLastDamage() <= damageTime && playerOpponents[1].getLastHit() <= damageTime) { // Make sure the player collecting the velocity from was recently hit
                self.getVelocity().collect(p, to);
            }
        } else if (Damage.getLastReceived(p) <= damageTime
                && p.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            VelocityCheck.collectEntity(p, to);
        }

        // Detections
        if (checkPlayer && !crawling) {
            HeadPosition.run(p, to, from); // Optimised
            ImpossibleInventory.runCursorUsage(p, from, ver); // Optimised
            KillAura.runOnMovement(p, to, from); // Optimised
        }
        UndetectedMovement.runMove(p, dis);

        if (!repetitionDistance
                && (dis == 0.0 || dis < 0.01 && ver == 0.0)
                && p.isOnGround() && p.isOnGroundCustom()) {
            return;
        }

        // Values
        int air = p.getTicksOnAir();
        Entity vehicle = p.getVehicle();
        boolean hasVehicle = vehicle != null;

        // Features
        if (Debug.canRun()) {
            Debug.inform(p, Enums.Debug.MOVEMENT, "distance: " + AlgebraUtils.cut(dis, 2) + ", "
                    + "vertical: " + AlgebraUtils.cut(ver, 2) + ", "
                    + "horizontal: " + AlgebraUtils.cut(hor, 2) + ", "
                    + "remaining: " + AlgebraUtils.cut(rem, 5) + ", "
                    + "air-ticks: " + air + ", "
                    + "ground(vanilla/custom): " + p.isOnGround() + "/" + p.isOnGroundCustom()
                    + (hasVehicle ? ", vehicle: " + CombatUtils.entityToString(vehicle) : ""));
        }

        // Utils
        MovementProcessing.run(p, to, vehicle, buffer, cooldowns, decimals, dis, hor, ver, rem, crawling); // Optimised

        // Detections
        if (checkPlayer && !crawling && (firstTick || repetition)) {
            if (firstTick) {
                ServerFlying.run(p);
            }
            ChunkUpdates.run(p, to, from, vehicle); // Optimised
            SprintSpeed.run(p, to, from, ver, hor); // Optimised
        }

        if (heavyMovementChecks) {
            if (checkPlayer && (firstTick || repetition || tick <= 3)) {
                double oHor = p.getOld_NmsHorizontalDistance(),
                        oBox = p.getOld_NmsBox(),
                        oVer = p.getOld_NmsVerticalDistance();

                // Detections
                if (firstTick || repetition) {
                    if (!crawling) {
                        NoFall.run(p, to, from, ver, oVer, hor, air); // Repetitive (Optimised)
                        NoSlowdown.run(p, to, from, ver, hor, oHor); // Repetitive (Optimised)
                        EntityMove.run(p, to, from, hor, ver, oVer, rem, oBox, air); // Repetitive (Optimised)
                    }
                    IrregularMovements.run(p, to, from, ver, oVer, hor, oHor, rem, oBox, air); // Repetitive (Optimised)
                }
                Speed.run(p, to, from, ver, hor, air, rem, oVer, oHor, oBox, crawling); // Important (Optimised)
                WaterSpeed.run(p, to, from, ver, oVer, rem, hor, air); // Repetitive (Optimised)
            }
            MorePackets.run(p, to, hor, ver); // Required (Let the check run in all circumstances to accurately count the packets)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void WorldChange(PlayerChangedWorldEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        // Object
        p.resetLocationData();
        p.setPlayerWeather(n.getPlayerWeather());

        // Detections
        CheckProtection.cancel(p.getUniqueId(), 20, true);

        // System
        Cache.clear(p, n, false, true, true, p.getLocation());
    }
}
