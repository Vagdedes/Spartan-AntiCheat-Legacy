package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import me.vagdedes.spartan.system.Enums;

public class Velocity extends CheckExecutor {

    public Velocity(SpartanPlayer player) {
        super(Enums.HackType.Velocity, player);
    }

    @Override
    public void runInternal(boolean cancelled) {
        if (canDo()) {
            // todo
        }
    }

    @Override
    public void schedulerInternal() {

    }

    @Override
    protected boolean canDo() {
        return CombatProcessing.canCheck(player)
                && !player.isOutsideOfTheBorder(-1.0);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {

    }
}
