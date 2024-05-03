package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;

public class MorePackets extends CheckExecutor {

    public static final HackType check = HackType.MorePackets;

    public MorePackets(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            // todo
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true);
    }
}