package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.EntityAttackPlayerEvent;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
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
            Enums.HackType.Criticals
    };

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Deal(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager(),
                entity = e.getEntity();
        boolean entityIsPlayer = entity instanceof Player,
                entityAttack = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;

        if (damager instanceof Player) {
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
            if (entityAttack && (entityIsPlayer || entity instanceof LivingEntity)) {
                deal(
                        new PlayerAttackEvent(
                                (Player) damager,
                                (LivingEntity) entity,
                                e.isCancelled()
                        ),
                        false
                );
            }
        }

        if (entityIsPlayer) {
            SpartanPlayer p = SpartanBukkit.getProtocol((Player) entity).spartanPlayer;

            // Objects
            p.addReceivedDamage(e);
        } else {
            handlePassengers(entity, false, e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Receive(EntityDamageEvent e) {
        receive(e, false);
    }

    public static void receive(EntityDamageEvent e, boolean packets) { // todo call in protocol
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            SpartanBukkit.getProtocol((Player) entity).spartanPlayer.addReceivedDamage(e);
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
                        protocol.spartanPlayer.addReceivedDamage(e);
                    }
                }
            }
        }
    }

    public static void deal(PlayerAttackEvent e, boolean packets) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            SpartanPlayer p = protocol.spartanPlayer;
            boolean cancelled = e.isCancelled();

            p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
            p.getExecutor(Enums.HackType.HitReach).handle(cancelled, e);
            p.getExecutor(Enums.HackType.KillAura).handle(cancelled, e);
        }
    }

    public static void receive(EntityAttackPlayerEvent e, boolean packets) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            protocol.spartanPlayer.getExecutor(Enums.HackType.KillAura).handle(e.isCancelled(), e);
        }
    }

}
