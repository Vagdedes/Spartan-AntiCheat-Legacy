package com.vagdedes.vacan.listeners.bukkit.standalone;

import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.functionality.server.MultiVersion;
import com.vagdedes.vacan.functionality.server.PluginBase;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class VehicleDeathEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void Event(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();

        if (entity instanceof Vehicle) {
            Entity[] passengers = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    ? entity.getPassengers().toArray(new Entity[0])
                    : new Entity[]{entity.getPassenger()};

            if (passengers.length > 0) {
                for (Entity passenger : passengers) {
                    if (passenger instanceof Player) {
                        PlayerProtocol p = PluginBase.getProtocol((Player) passenger, true);
                        p.profile().executeRunners(
                                false,
                                new VehicleExitEvent(
                                        (Vehicle) entity,
                                        (Player) passenger,
                                        false
                                )
                        );
                    }
                }
            }
        }
    }

}
