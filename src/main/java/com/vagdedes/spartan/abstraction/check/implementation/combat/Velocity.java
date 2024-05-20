package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import me.vagdedes.spartan.system.Enums;

public class Velocity extends CheckExecutor {

    public Velocity(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    @Override
    protected void runInternal(boolean cancelled) {
        // todo
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player)
                && !player.isOutsideOfTheBorder(-1.0);
    }
}
