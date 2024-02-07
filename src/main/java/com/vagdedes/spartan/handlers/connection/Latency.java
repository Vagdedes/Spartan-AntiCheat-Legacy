package com.vagdedes.spartan.handlers.connection;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.entity.Player;

public class Latency {

    public static int ping(Player p) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return p.getPing();
        } else {
            try {
                Object obj = SpartanBukkit.getCraftPlayerMethod(p, "ping");

                if (obj instanceof Integer) {
                    return Math.max((int) obj, 0);
                } else {
                    return 0;
                }
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

}
