package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.abilities.mcMMO;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.Shared;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class Event_Combat implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Damage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager(),
                entity = e.getEntity();
        boolean entityIsPlayer = entity instanceof Player;

        if (damager instanceof Player) {
            boolean entityAttack = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;

            if (entityAttack
                    && entity instanceof LivingEntity
                    && !SpartanBukkit.packetsEnabled()) {
                Shared.attack(new PlayerAttackEvent(
                        (Player) damager,
                        (LivingEntity) entity,
                        e.isCancelled())
                );
            }
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) damager);

            if (p == null) {
                return;
            }
            // Object
            p.addDealtDamage(e);

            // Detections
            if (entityAttack) {
                p.calculateClicks(null, true);

                if (entityIsPlayer || entity instanceof LivingEntity) {
                    boolean cancelled = e.isCancelled();

                    if (!p.uuid.equals(entity.getUniqueId())
                            && !mcMMO.hasGeneralAbility(p)
                            && !MythicMobs.is(entity)
                            && !ItemsAdder.is(entity)) {
                        // Detections
                        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
                        p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
                        p.getExecutor(Enums.HackType.Criticals).handle(cancelled, e);

                        if (!cancelled) {
                            // Object (Always after detections so to not refresh last-hit, last-damage, e.t.c.)
                            if (entityIsPlayer) {
                                SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                                if (target != null) {
                                    p.getProfile().playerCombat.getFight(target).update(p);
                                }
                            }
                        }

                        if (p.getViolations(Enums.HackType.KillAura).prevent()
                                || p.getViolations(Enums.HackType.Criticals).prevent()
                                || p.getViolations(Enums.HackType.NoSwing).prevent()
                                || p.getViolations(Enums.HackType.FastClicks).prevent()) {
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }

        if (entityIsPlayer) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

            if (p == null) {
                return;
            }
            // Objects
            p.addReceivedDamage(e);

            // Detections
            p.getExecutor(Enums.HackType.Speed).handle(e.isCancelled(), e);
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        SpartanPlayer p = SpartanBukkit.getPlayer((Player) passenger);

                        if (p != null) {
                            // Objects
                            p.addReceivedDamage(e);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Damage(EntityDamageEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            // Objects
            p.addReceivedDamage(e);

            // Detections
            p.getExecutor(Enums.HackType.NoFall).handle(e.isCancelled(), e.getCause());
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        Player n = (Player) passenger;
                        SpartanPlayer p = SpartanBukkit.getPlayer(n);

                        if (p != null) {
                            // Objects
                            p.addReceivedDamage(e);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BowShot(EntityShootBowEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) e.getEntity());

            if (p == null) {
                return;
            }
            // Detections
            p.getExecutor(Enums.HackType.FastBow).handle(e.isCancelled(), e);

            if (p.getViolations(Enums.HackType.FastBow).prevent()) {
                e.setCancelled(true);
            }
        }
    }

}
