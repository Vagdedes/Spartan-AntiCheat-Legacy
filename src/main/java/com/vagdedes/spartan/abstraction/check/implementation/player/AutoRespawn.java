package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.PluginUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameRule;

public class AutoRespawn extends CheckRunner {

    private final CheckDetection detection;
    private long memory;

    public AutoRespawn(HackType hackType, SpartanProtocol protocol, String playerName) {
        super(hackType, protocol, playerName);
        this.detection = new ImplementedProbabilityDetection(this, null, true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        this.memory = System.currentTimeMillis();
    }

    @Override
    protected void runInternal(boolean cancelled) {
        long time = System.currentTimeMillis() - this.memory,
                limit = 750L;

        if (time <= limit && !this.protocol().bukkit.isDead()) {
            memory = 0L;
            this.detection.cancel(
                    "type: default, ms: " + time,
                    this.protocol().getFromLocation(),
                    AlgebraUtils.integerCeil(TPS.maximum)
            );
        }
    }

    @Override
    protected boolean canRun() {
        return !PluginUtils.contains("respawn")
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                || !this.protocol().getWorld().getGameRuleValue(GameRule.DO_IMMEDIATE_RESPAWN) == true);
    }
}
