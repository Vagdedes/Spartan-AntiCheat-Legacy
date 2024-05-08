package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;

public class NoFall extends CheckExecutor {

    public static final HackType check = HackType.NoFall;

    public NoFall(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        player.getCooldowns(hackType).add(new String[]{"land", "ratio"}, 10);
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            player.debug(true, false, false, player.getFallDistance());
        }
        return false;
    }

    @Override
    public void scheduler() {

    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && MovementProcessing.canCheck(player, false, false, false)
                && player.getVehicle() == null
                && !player.movement.wasInLiquids()
                && !BlockUtils.canClimb(player.movement.getLocation().getBlock().material);
    }
}
