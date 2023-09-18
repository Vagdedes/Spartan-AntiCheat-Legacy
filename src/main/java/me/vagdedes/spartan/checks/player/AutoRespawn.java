package me.vagdedes.spartan.checks.player;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.handlers.stability.Cache;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.PluginUtils;
import org.bukkit.GameRule;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoRespawn extends CheckExecutor {

    private static final HackType check = HackType.AutoRespawn;
    private static final long limit = 750L;
    private static final Map<UUID, Long> hm = Cache.store(new ConcurrentHashMap<>(Config.getMaxPlayers()));

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (!hm.isEmpty()) {
                    for (Map.Entry<UUID, Long> entry : hm.entrySet()) {
                        long time = System.currentTimeMillis() - entry.getValue();

                        if (time <= limit) {
                            SpartanPlayer p = SpartanBukkit.getPlayer(entry.getKey());

                            if (p != null) {
                                SpartanBukkit.runTask(p, () -> {
                                    if (p != null && !p.isDead()) {
                                        p.getExecutor(check).handle(time);
                                    }
                                });
                            }
                        }
                    }
                    hm.clear();
                }
            }, 1L, 1L);
        }
    }

    public AutoRespawn(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean handle(Object object) {
        cancel("type: default, ms: " + object);
        return false;
    }

    @Override
    public boolean run() {
        if (canDo()) {
            hm.put(player.getUniqueId(), System.currentTimeMillis());
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, false)
                && !PluginUtils.contains("respawn")
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                || !player.getWorld().getGameRuleValue(GameRule.DO_IMMEDIATE_RESPAWN) == true);
    }
}
