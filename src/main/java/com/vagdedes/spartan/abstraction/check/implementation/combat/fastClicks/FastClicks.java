package com.vagdedes.spartan.abstraction.check.implementation.combat.fastClicks;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums;

public class FastClicks extends CheckExecutor {

    static final int defaultClicksPerSecondLimit = 15;

    private final ClickPattern clickPattern;
    private final ClicksPerSecond clicksPerSecond;

    public FastClicks(SpartanPlayer player) {
        super(Enums.HackType.FastClicks, player, 2);
        this.clickPattern = new ClickPattern(this);
        this.clicksPerSecond = new ClicksPerSecond(this);
        this.addDetections(
                new DetectionExecutor[]{
                        this.clickPattern,
                        this.clicksPerSecond
                }
        );
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !player.getTrackers().has(Trackers.TrackerType.SensitiveBlockBreak)
                && !PlayerUtils.isPickaxeItem(player.getItemInHand().getType())
                && !PrinterMode.canCancel(player);
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return canDo() && (this.clicksPerSecond.run() || this.clickPattern.run());
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }
}
