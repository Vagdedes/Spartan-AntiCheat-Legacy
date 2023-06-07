package me.vagdedes.spartan.handlers.connection;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.entity.Player;

public class Latency {

    public static final int
            maxPing = 2000,
            criticalDifference = 33,
            maxDifference = (criticalDifference * 3) + 1;

    public static boolean canUseProtection() {
        int support = Settings.getInteger("Protections.max_supported_player_latency");
        return support > 0 && support <= maxPing;
    }

    public static int ping(Player p) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return p.getPing();
        } else {
            try {
                Object obj = SpartanBukkit.getCraftPlayerMethod(p, "ping");
                return Math.max((int) obj, 0);
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

}
