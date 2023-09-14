package me.vagdedes.spartan.checks.implementation.player;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.GameRule;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoRespawn {

    private static final HackType check = HackType.AutoRespawn;
    private static final long limit = 750L;
    private static final Map<UUID, Long> hm = Cache.store(new ConcurrentHashMap<>(Config.getMaxPlayers()));

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (hm.size() > 0) {
                    for (Map.Entry<UUID, Long> entry : hm.entrySet()) {
                        long time = System.currentTimeMillis() - entry.getValue();

                        if (time <= limit) {
                            SpartanPlayer p = SpartanBukkit.getPlayer(entry.getKey());

                            SpartanBukkit.runTask(p, () -> {
                                if (p != null && !p.isDead()) {
                                    new HackPrevention(p, check, "type: default, ms: " + time);
                                }
                            });
                        }
                    }
                    hm.clear();
                }
            }, 1L, 1L);
        }
    }

    public static void handleDeath(SpartanPlayer p) {
        if (p.canRunCheck(check, false) && !PluginUtils.contains("respawn")
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) || !p.getWorld().getGameRuleValue(GameRule.DO_IMMEDIATE_RESPAWN) == true)) {
            hm.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }
}
