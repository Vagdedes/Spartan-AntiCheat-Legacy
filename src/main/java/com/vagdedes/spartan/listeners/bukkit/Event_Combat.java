package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.EntityAttackPlayerEvent;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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

    private static final Enums.HackType[] handledChecks = new Enums.HackType[]{
            Enums.HackType.KillAura,
            Enums.HackType.HitReach,
            Enums.HackType.NoSwing,
            Enums.HackType.Criticals,
            Enums.HackType.FastClicks
    };

    @EventHandler(priority = EventPriority.HIGHEST)
    private void DealAndReceive(EntityDamageByEntityEvent e) {
        dealAndReceive(e, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Receive(EntityDamageEvent e) {
        receive(e, false);
    }

    public static void dealAndReceive(EntityDamageByEntityEvent e, boolean packets) {
        Entity damager = e.getDamager(),
                entity = e.getEntity();
        boolean damagerIsPlayer = damager instanceof Player,
                entityIsPlayer = entity instanceof Player,
                entityAttack = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;

        if (damagerIsPlayer) {
            Player player = (Player) damager;
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

            if (protocol.packetsEnabled() == packets) {
                // Detections
                if (entityAttack) {
                    protocol.spartanPlayer.calculateClicks(true);

                    if (entityIsPlayer || entity instanceof LivingEntity) {
                        boolean cancelled = e.isCancelled();
                        PlayerAttackEvent event = new PlayerAttackEvent(
                                player,
                                (LivingEntity) entity,
                                cancelled
                        );
                        protocol.spartanPlayer.getExecutor(Enums.HackType.NoSwing).handle(cancelled, event);
                        protocol.spartanPlayer.getExecutor(Enums.HackType.HitReach).handle(cancelled, event);
                        protocol.spartanPlayer.getExecutor(Enums.HackType.KillAura).handle(cancelled, event);

                        for (Enums.HackType hackType : handledChecks) {
                            if (protocol.spartanPlayer.getExecutor(hackType).prevent()) {
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }

        if (entityIsPlayer) {
            Player player = (Player) entity;
            SpartanProtocol protocol = SpartanBukkit.getProtocol(player);

            if (protocol.packetsEnabled() == packets) {
                // Objects
                protocol.spartanPlayer.handleReceivedDamage(e);

                // Detections
                if (entityAttack && (damagerIsPlayer || damager instanceof LivingEntity)) {
                    boolean cancelled = e.isCancelled();
                    EntityAttackPlayerEvent event = new EntityAttackPlayerEvent(
                            player,
                            (LivingEntity) damager,
                            cancelled
                    );
                    protocol.spartanPlayer.getExecutor(Enums.HackType.KillAura).handle(cancelled, event);
                }
            }
        } else {
            handlePassengers(entity, packets, e);
        }
    }

    public static void receive(EntityDamageEvent e, boolean packets) { // todo call in protocol
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanProtocol protocol = SpartanBukkit.getProtocol((Player) entity);

            if (protocol.packetsEnabled() == packets) {
                protocol.spartanPlayer.handleReceivedDamage(e);
            }
        } else {
            handlePassengers(entity, packets, e);
        }
    }

    private static void handlePassengers(Entity entity, boolean packets, EntityDamageEvent e) {
        Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                ? entity.getPassengers().toArray(new Entity[0])
                : new Entity[]{entity.getPassenger()};

        if (passengers.length > 0) {
            for (Entity passenger : passengers) {
                if (passenger instanceof Player) {
                    // Objects
                    SpartanProtocol protocol = SpartanBukkit.getProtocol((Player) passenger);

                    if (protocol.packetsEnabled() == packets) {
                        protocol.spartanPlayer.handleReceivedDamage(e);
                    }
                }
            }
        }
    }

}
