package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.PluginUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameRule;

public class AutoRespawn extends CheckExecutor {

    private final DetectionExecutor detection;
    private long memory;

    public AutoRespawn(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.detection = new ImplementedDetectionExecutor(this, null, true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        this.memory = System.currentTimeMillis();
    }

    @Override
    protected void runInternal(boolean cancelled) {
        long time = System.currentTimeMillis() - this.memory,
                limit = 750L;

        if (time <= limit && !player.getInstance().isDead()) {
            memory = 0L;
            this.detection.cancel(
                    "type: default, ms: " + time,
                    (limit - time) / TPS.tickTimeDecimal,
                    player.movement.getDetectionLocation(),
                    AlgebraUtils.integerCeil(TPS.maximum)
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
