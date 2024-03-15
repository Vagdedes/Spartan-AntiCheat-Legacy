package com.vagdedes.spartan.checks.player;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameRule;

import java.util.*;

public class AutoRespawn extends CheckExecutor {

    private static final HackType check = HackType.AutoRespawn;
    private static final long limit = 750L;
    private static final Map<UUID, Long> memory
            = Cache.store(Collections.synchronizedMap(new LinkedHashMap<>(Config.getMaxPlayers())));

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (!memory.isEmpty()) {
                    synchronized (memory) {
                        Iterator<Map.Entry<UUID, Long>> iterator = memory.entrySet().iterator();

                        while (iterator.hasNext()) {
                            Map.Entry<UUID, Long> entry = iterator.next();
                            long time = System.currentTimeMillis() - entry.getValue();

                            if (time <= limit) {
                                SpartanPlayer p = SpartanBukkit.getPlayer(entry.getKey());

                                if (p != null) {
                                    SpartanBukkit.runTask(p, () -> {
                                        if (p != null && !p.isDead()) {
                                            p.getExecutor(check).handle(false, time);
                                        }
                                    });
                                }
                            }
                            iterator.remove();
                        }
                    }
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
            long time = System.currentTimeMillis();

            synchronized (memory) {
                memory.put(player.uuid, time);
            }
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
