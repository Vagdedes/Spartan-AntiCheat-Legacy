package com.vagdedes.spartan.listeners.omnibridge;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.player.PlayerJoinEvent;

public class ProtocolEvents {

    public static void join(SpartanPlayer p, PlayerJoinEvent e) {
        p.getExecutor(Enums.HackType.Speed).handle(false, e);
    }
}
