package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import me.vagdedes.spartan.system.Enums.HackType;

public class HitReach extends CheckExecutor {

    public HitReach(SpartanPlayer player) {
        super(HackType.HitReach, player);
    }

    @Override
    public void runInternal(boolean cancelled) {
        // todo
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player);
    }
}
