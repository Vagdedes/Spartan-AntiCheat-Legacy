package com.vagdedes.vacan.listeners.bukkit;

import com.vagdedes.vacan.abstraction.event.PlayerTickEvent;
import com.vagdedes.vacan.abstraction.event.PlayerTransactionEvent;
import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.abstraction.world.SpartanLocation;
import com.vagdedes.vacan.functionality.server.PluginBase;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void MoveEvent(PlayerMoveEvent e) {
        event(e, false);
    }

    public static void event(PlayerMoveEvent e, boolean packets) {
        PlayerProtocol protocol = PluginBase.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            Location nto = e.getTo();

            if (nto == null) {
                return;
            }
            Location vehicle = protocol.getVehicleLocation();
            SpartanLocation to = vehicle != null
                    ? new SpartanLocation(vehicle)
                    : new SpartanLocation(nto);

            if (!protocol.processLastMoveEvent(
                    nto,
                    vehicle,
                    to,
                    e.getFrom(),
                    packets
            )) {
                return;
            }
            protocol.profile().executeRunners(e.isCancelled(), e);
        }
    }

    public static void tick(PlayerTickEvent tickEvent) {
        PlayerProtocol protocol = tickEvent.protocol;
        protocol.lastTickEvent = tickEvent;
        protocol.packetWorld.tick(tickEvent);
        protocol.profile().executeRunners(false, tickEvent);
    }

    public static void transaction(PlayerTransactionEvent event) {
        PlayerProtocol protocol = event.protocol;
        protocol.profile().executeRunners(false, event);
    }

}
