package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.server.PluginUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameRule;

public class AutoRespawn extends CheckExecutor {

    private long memory;

    public AutoRespawn(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.memory = 0L;
    }

    @Override
    protected void runInternal(boolean cancelled) {
        memory = System.currentTimeMillis();
    }

    @Override
    public void schedulerInternal() {
        long time = System.currentTimeMillis() - memory,
                limit = 750L;

        if (time <= limit && !player.getInstance().isDead()) {
            memory = 0L;
            cancel(
                    "type: default, ms: " + time,
                    (limit - time) / TPS.tickTimeDecimal,
                    player.movement.getDetectionLocation(),
                    (int) TPS.maximum
            );
        }
    }

    @Override
    protected boolean canRun() {
        return !PluginUtils.contains("respawn")
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                || !player.getWorld().getGameRuleValue(GameRule.DO_IMMEDIATE_RESPAWN) == true);
    }
}
