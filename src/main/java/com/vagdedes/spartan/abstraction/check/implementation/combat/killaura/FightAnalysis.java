package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class FightAnalysis extends DetectionExecutor {

    FightAnalysis(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void runDamage() {
        if (hackType.getCheck().getBooleanOption("check_fight_analysis", true)) {
            // todo hits vs damages statistics
        }
    }
}
