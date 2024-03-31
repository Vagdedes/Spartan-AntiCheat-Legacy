package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.chat.ChatProtection;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.Explosion;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.FloorProtection;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TestServer;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;

public class EventsHandler6 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Command(PlayerCommandPreprocessEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        String msg = e.getMessage();

        if (ChatProtection.runCommand(p, msg, false)) {
            e.setCancelled(true);
        } else {
            // Detections
            p.getExecutor(Enums.HackType.ItemDrops).handle(e.isCancelled(), msg);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void KickEvent(PlayerKickEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        // System
        if (!TestServer.isIdentified() && Config.settings.getBoolean("Important.violations_reset_on_kick")) {
            for (Enums.HackType hackType : Enums.HackType.values()) {
                p.getViolations(hackType).reset();
            }
        }
    }

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST)
    private void DealDamage(EntityDamageByEntityEvent e) {
        Entity en = e.getDamager();

        if (en instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) en);

            if (p == null) {
                return;
            }
            // Object
            p.addDealtDamage(e);

            // Detections
            if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                Entity defaultEntity = e.getEntity();
                boolean hitCooldown = p.getTimer().get("hit-cooldown") <= 5L;
                p.getTimer().set("hit-cooldown");

                if (!hitCooldown) {
                    p.calculateClicks(null, true);
                }

                if (defaultEntity instanceof LivingEntity) {
                    boolean cancelled = e.isCancelled();
                    LivingEntity entity = (LivingEntity) defaultEntity;

                    if (!cancelled) {
                        // Compatibility
                        NoHitDelay.runDealDamage(p, entity);
                    }
                    if (!p.uuid.equals(defaultEntity.getUniqueId())
                            && !mcMMO.hasGeneralAbility(p)
                            && !MythicMobs.is(defaultEntity)
                            && !ItemsAdder.is(defaultEntity)) {
                        double[] utility = CombatUtils.get_X_Y_Distance(p, entity);

                        // Detections
                        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
                        p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
                        p.getExecutor(Enums.HackType.Criticals).handle(cancelled, new Object[]{e.getDamage(), entity});

                        if (utility != null
                                && (!hitCooldown
                                || CombatUtils.isNewPvPMechanic(p, entity))) {
                            Object[] objects = new Object[]{entity, utility};
                            p.getExecutor(Enums.HackType.KillAura).run(cancelled);
                            p.getExecutor(Enums.HackType.KillAura).handle(cancelled, objects);
                            p.getExecutor(Enums.HackType.HitReach).handle(cancelled, objects);
                        }
                        if (!cancelled) {
                            // Object (Always after detections so to not refresh last-hit, last-damage, e.t.c.)
                            if (entity instanceof Player) {
                                SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                                if (target != null) {
                                    p.getProfile().playerCombat.getFight(target).update(p);
                                }
                            }
                        }
                    }
                }
            }

            if (p.getViolations(Enums.HackType.KillAura).process()
                    || p.getViolations(Enums.HackType.Criticals).process()
                    || p.getViolations(Enums.HackType.NoSwing).process()
                    || p.getViolations(Enums.HackType.FastClicks).process()) {
                e.setCancelled(true);
            }
        } else {
            Entity entity = e.getEntity();

            if (entity instanceof Player) {
                Player n = (Player) entity;
                SpartanPlayer p = SpartanBukkit.getPlayer(n);

                if (p == null) {
                    return;
                }
                // Objects
                p.addReceivedDamage(e);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void ReceiveDamage(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();

        if (damager != null && entity != null) {
            boolean cancelled = e.isCancelled();
            EntityDamageEvent.DamageCause cause = e.getCause();

            // Protections
            if (Damage.runDealAndReceiveDamage(damager, entity, cause, cancelled)) {
                e.setCancelled(true);
            } else {
                if (!damager.equals(entity)) {
                    if (entity instanceof Player) {
                        SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

                        if (p == null) {
                            return;
                        }
                        GameMode gameMode = p.getGameMode();

                        if (cancelled && (gameMode == GameMode.SURVIVAL
                                || gameMode == GameMode.ADVENTURE)) {
                            return;
                        }
                        // Handlers
                        Damage.runReceiveDamage(cause, p, damager);
                        Explosion.runDamage(p, damager, cause);
                        FloorProtection.runReceiveDamage(p, damager, cause);
                    } else {
                        Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? entity.getPassengers().toArray(new Entity[0]) : new Entity[]{entity.getPassenger()};

                        if (passengers.length > 0) {
                            for (Entity passenger : passengers) {
                                if (passenger instanceof Player) {
                                    SpartanPlayer p = SpartanBukkit.getPlayer((Player) passenger);

                                    if (p != null) {
                                        // Handlers
                                        Damage.runReceiveDamage(cause, p, damager);
                                        Explosion.runDamage(p, damager, cause);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Animation(PlayerAnimationEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        PlayerAnimationType animationType = e.getAnimationType();

        // Detections
        p.getExecutor(Enums.HackType.NoSwing).handle(e.isCancelled(), e);
    }
}
