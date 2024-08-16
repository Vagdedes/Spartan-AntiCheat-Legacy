package com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;

public class FastClicks extends CheckExecutor {

    private final ClickPatternML clickPattern;
    private final ClicksPerSecond clicksPerSecond;

    public FastClicks(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 2, false, false);
        this.addDetections(
                new DetectionExecutor[]{
                        this.clickPattern = new ClickPatternML(this),
                        this.clicksPerSecond = new ClicksPerSecond(this)
                }
        );
    }

    @Override
    protected void runInternal(boolean cancelled) {
        this.clicksPerSecond.run();
        this.clickPattern.run();
    }
}
