package com.vagdedes.spartan.checks.player;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.handlers.stability.Cache;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.system.Enums.HackType;
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
    public boolean handleInternal(boolean cancelled, Object object) {
        cancel("type: default, ms: " + object);
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
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
