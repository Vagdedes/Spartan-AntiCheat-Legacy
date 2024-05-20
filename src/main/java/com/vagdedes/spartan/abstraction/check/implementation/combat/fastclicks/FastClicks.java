package com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.building.PrinterMode;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.system.Enums;

public class FastClicks extends CheckExecutor {

    static final int defaultClicksPerSecondLimit = 15;

    private final ClickPattern clickPattern;
    private final ClicksPerSecond clicksPerSecond;

    public FastClicks(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 2);
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
    protected boolean canRun() {
        return !PlayerUtils.isPickaxeItem(player.getItemInHand().getType())
                && !PrinterMode.isUsing(player);
    }

    @Override
    protected void runInternal(boolean cancelled) {
        this.clicksPerSecond.run();
        this.clickPattern.run();
    }
}
