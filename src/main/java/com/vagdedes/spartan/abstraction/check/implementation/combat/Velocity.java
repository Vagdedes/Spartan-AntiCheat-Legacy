package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.system.Enums;

public class Velocity extends CheckExecutor {

    private static final Enums.HackType check = Enums.HackType.Velocity;

    public Velocity(SpartanPlayer player) {
        super(check, player);
    }

    private static final double[][]
            solid = new double[][]{
            {0, 1, 0},
            {0, 2, 0}
    }, web = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max}
    }, semiSolid = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 2, BlockUtils.hitbox_max}
    };

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            // todo
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return !player.canRunCheck(hackType, true)
                && CombatProcessing.canCheck(player)
                && !player.isOutsideOfTheBorder(-1.0);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }
}
