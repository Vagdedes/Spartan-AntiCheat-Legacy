package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Event_Velocity implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void event(PlayerVelocityEvent e) {
        SpartanPlayer p = SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer;

        // Object
        if (!e.isCancelled()) {
            p.trackers.add(
                    Trackers.TrackerType.ABSTRACT_VELOCITY,
                    (int) (Math.ceil(e.getVelocity().length()) * TPS.maximum)
            );
        }

        // Detections
        boolean cancelled = e.isCancelled();
        p.getExecutor(Enums.HackType.Speed).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Velocity).handle(cancelled, e);
        p.getExecutor(Enums.HackType.Simulation).handle(cancelled, e);
        p.getExecutor(Enums.HackType.NoFall).handle(false, null);
    }

}
