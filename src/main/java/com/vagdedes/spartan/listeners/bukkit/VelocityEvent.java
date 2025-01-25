package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.event.CPlayerVelocityEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;

public class VelocityEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerVelocityEvent e) {
        event(
                new CPlayerVelocityEvent(
                        e.getPlayer(),
                        e.getVelocity()
                ),
                false
        );
    }

    public static void event(CPlayerVelocityEvent e, boolean packets) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        if (protocol.packetsEnabled() == packets) {
            protocol.claimedVelocity = e;
            protocol.claimedVeloGravity.add(e);
            if (protocol.claimedVeloGravity.size() > 2)
                protocol.claimedVeloGravity.remove(0);
            protocol.claimedVeloSpeed.add(e);
            if (protocol.claimedVeloSpeed.size() > 2)
                protocol.claimedVeloSpeed.remove(0);

            // Detections
            boolean cancelled = e.isCancelled();
            protocol.profile().getRunner(Enums.HackType.IrregularMovements).handle(cancelled, e);
            protocol.profile().getRunner(Enums.HackType.Velocity).handle(cancelled, e);
        }
    }

}
