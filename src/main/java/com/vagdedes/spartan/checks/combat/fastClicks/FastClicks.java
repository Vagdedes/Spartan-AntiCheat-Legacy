package com.vagdedes.spartan.checks.combat.fastClicks;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums;

public class FastClicks extends CheckExecutor {

    private static final int defaultClicksPerSecondLimit = 15;

    public FastClicks(SpartanPlayer player) {
        super(Enums.HackType.FastClicks, player);
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !player.getHandlers().has(Handlers.HandlerType.SensitiveBlockBreak)
                && !PlayerUtils.isPickaxeItem(player.getItemInHand().getType())
                && !PrinterMode.canCancel(player);
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            int limit = hackType.getCheck().getNumericalOption("clicks_per_second_limit", defaultClicksPerSecondLimit);
            int count = player.getClicks().getCount();

            if (count >= limit) {
                cancel(
                        "type: clicks-per-second"
                                + ", count: " + count
                                + ", limit: " + limit
                );
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }
}
