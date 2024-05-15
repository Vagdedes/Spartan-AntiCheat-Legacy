package com.vagdedes.spartan.listeners.omnibridge;

import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.MaximumCheckedPlayers;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class ProtocolEvents {
    public static void Join(PlayerJoinEvent e) {
        Player n = e.getPlayer();
        SpartanBukkit.addRealPlayer(n);

        // Utils
        if (PlayerLimitPerIP.add(n)) {
            e.setJoinMessage(null);
            return;
        }
        SpartanPlayer p = SpartanBukkit.getPlayer(n);

        if (p == null) {
            return;
        }
        // System
        MaximumCheckedPlayers.add(p);
        SpartanEdition.attemptNotification(p);

        // Detection
        p.getExecutor(Enums.HackType.Speed).handle(false, e);

        SpartanBukkit.runDelayedTask(p, () -> {
            if (p != null) {
                Config.settings.runOnLogin(p);
                CloudBase.announce(p);
            }
        }, 10);
    }
}
