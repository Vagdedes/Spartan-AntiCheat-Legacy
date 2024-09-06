package com.vagdedes.spartan.listeners.bukkit.standalone;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class Event_Leave implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Event(PlayerQuitEvent e) {
        Player n = e.getPlayer();

        if (ProtocolLib.isTemporary(n)) {
            return;
        }
        SpartanProtocol protocol = SpartanBukkit.deleteProtocol(n);

        if (protocol == null) {
            return;
        }
        SpartanPlayer p = protocol.spartanPlayer;

        // Features
        DetectionNotifications.runOnLeave(p);
    }

}
