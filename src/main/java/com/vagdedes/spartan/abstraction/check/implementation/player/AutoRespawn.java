package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameRule;

public class AutoRespawn extends CheckExecutor {

    private long memory;

    public AutoRespawn(SpartanPlayer player) {
        super(HackType.AutoRespawn, player);
        this.memory = 0L;
    }

    @Override
    public void runInternal(boolean cancelled) {
        memory = System.currentTimeMillis();
    }

    @Override
    public void schedulerInternal() {
        long time = System.currentTimeMillis() - memory;

        if (time <= 750L && !player.isDead()) {
            memory = 0L;
            cancel("type: default, ms: " + time);
        }
    }

    @Override
    protected boolean canRun() {
        return !PluginUtils.contains("respawn")
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                || !player.getWorld().getGameRuleValue(GameRule.DO_IMMEDIATE_RESPAWN) == true);
    }
}
