package com.vagdedes.spartan.listeners.bukkit.protocol.replicated;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.ReplicatedHandlers;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class Event_Movement implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerMoveEvent e) {
        SpartanProtocol protocol = SpartanBukkit.getProtocol(e.getPlayer());

        if (!protocol.packetsEnabled()) {
            ReplicatedHandlers.movement(e, false);
        }
        protocol.spartanPlayer.getExecutor(
                Enums.HackType.MorePackets
        ).handle(e.isCancelled(), null);
    }

}
