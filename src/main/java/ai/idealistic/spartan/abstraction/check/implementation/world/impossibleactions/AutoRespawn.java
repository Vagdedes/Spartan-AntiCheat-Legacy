package ai.idealistic.spartan.abstraction.check.implementation.world.impossibleactions;

import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.TPS;
import ai.idealistic.spartan.utils.math.AlgebraUtils;
import ai.idealistic.spartan.utils.minecraft.server.PluginUtils;
import org.bukkit.GameRule;

public class AutoRespawn extends CheckDetection {

    private long memory;

    AutoRespawn(CheckRunner executor) {
        super(executor, null, Check.DetectionType.BUKKIT, "auto_respawn", true);
    }

    void run() {
        this.call(() -> {
            if (!this.canRun()) {
                return;
            }
            long time = System.currentTimeMillis() - this.memory,
                    limit = 750L;

            if (time <= limit && !this.protocol.bukkit().isDead()) {
                memory = 0L;
                this.cancel(
                        "auto-respawn, ms: " + time,
                        this.protocol.getFromLocation(),
                        AlgebraUtils.integerCeil(TPS.maximum)
                );
            }
        });
    }

    void death() {
        this.memory = System.currentTimeMillis();
    }

    private boolean canRun() {
        return !PluginUtils.contains("respawn")
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                || !(this.protocol.getWorld().getGameRuleValue(GameRule.DO_IMMEDIATE_RESPAWN) == true));
    }

}
