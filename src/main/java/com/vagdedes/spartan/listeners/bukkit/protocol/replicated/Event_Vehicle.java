package com.vagdedes.spartan.listeners.bukkit.protocol.replicated;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class Event_Vehicle implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void death(EntityDeathEvent e) {
        Entity entity = e.getEntity();
        Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                ? entity.getPassengers().toArray(new Entity[0])
                : new Entity[]{entity.getPassenger()};

        if (passengers.length > 0) {
            Enums.HackType[] hackTypes = new Enums.HackType[]{
                    Enums.HackType.NoFall,
                    Enums.HackType.IrregularMovements,
            };

            for (Entity passenger : passengers) {
                if (passenger instanceof Player) {
                    SpartanPlayer p = SpartanBukkit.getProtocol((Player) passenger).spartanPlayer;

                    for (Enums.HackType hackType : hackTypes) {
                        p.getExecutor(hackType).addDisableCause(
                                hackType.toString(),
                                null,
                                1
                        );
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void enter(VehicleEnterEvent e) {
        Entity entity = e.getEntered();

        if (entity instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getProtocol((Player) entity).spartanPlayer;
            p.trackers.add(Trackers.TrackerType.VEHICLE, "enter", 5);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void exit(VehicleExitEvent e) {
        Entity en = e.getExited();

        if (en instanceof Player) {
            SpartanPlayer p = SpartanBukkit.getProtocol((Player) en).spartanPlayer;
            p.trackers.add(Trackers.TrackerType.VEHICLE, "exit", 5);
        }
    }

}
