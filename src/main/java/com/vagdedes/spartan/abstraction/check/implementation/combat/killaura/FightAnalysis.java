package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.profiling.PlayerFight;

public class FightAnalysis extends DetectionExecutor {

    FightAnalysis(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(PlayerFight fight) {
        if (hackType.getCheck().getBooleanOption("check_fight_analysis", true)) {
            runAsync(() -> {
                // todo
            });
        }
    }
}
