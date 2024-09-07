package com.vagdedes.spartan.listeners.bukkit.protocol.replicated;

import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.CustomHandlers;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class Event_Combat implements Listener {

    public static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            Enums.HackType.KillAura,
            Enums.HackType.HitReach,
            Enums.HackType.NoSwing,
            Enums.HackType.Criticals
    };

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void deal(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager(),
                entity = e.getEntity();
        boolean entityIsPlayer = entity instanceof Player;

        if (damager instanceof Player) {
            boolean entityAttack = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
            SpartanPlayer p = SpartanBukkit.getProtocol((Player) damager).spartanPlayer;

            // Object
            p.addDealtDamage(e);

            // Detections
            if (entityAttack) {
                p.calculateClicks(true);

                if (entityIsPlayer || entity instanceof LivingEntity) {
                    for (Enums.HackType hackType : handledChecks) {
                        if (p.getExecutor(hackType).prevent()) {
                            e.setCancelled(true);
                        }
                    }
                    if (p.getExecutor(Enums.HackType.FastClicks).prevent()) {
                        e.setCancelled(true);
                    }
                }
            }
            if (entityAttack
                    && (entityIsPlayer
                    && !SpartanBukkit.getProtocol((Player) entity).packetsEnabled()
                    || entity instanceof LivingEntity
                    && !SpartanBukkit.packetsEnabled())) {
                CustomHandlers.attack(new PlayerAttackEvent(
                        (Player) damager,
                        (LivingEntity) entity,
                        e.isCancelled()
                ));
            }
        }

        if (entityIsPlayer) {
            SpartanPlayer p = SpartanBukkit.getProtocol((Player) entity).spartanPlayer;
            boolean cancelled = e.isCancelled();

            // Objects
            p.addReceivedDamage(e);

            // Detections
            p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        // Objects
                        SpartanBukkit.getProtocol((Player) passenger).spartanPlayer.addReceivedDamage(e);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void receive(EntityDamageEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            // Objects
            SpartanBukkit.getProtocol((Player) entity).spartanPlayer.addReceivedDamage(e);
        } else {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        // Objects
                        SpartanBukkit.getProtocol((Player) passenger).spartanPlayer.addReceivedDamage(e);
                    }
                }
            }
        }
    }

}
