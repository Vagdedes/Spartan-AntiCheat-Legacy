package com.vagdedes.spartan.listeners.bukkit.protocol.replicated;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.TridentUse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRiptideEvent;

public class Event_Trident implements Listener {

    @EventHandler
    public static void event(PlayerRiptideEvent e) {
        TridentUse.run(SpartanBukkit.getProtocol(e.getPlayer()).spartanPlayer);
    }

}
