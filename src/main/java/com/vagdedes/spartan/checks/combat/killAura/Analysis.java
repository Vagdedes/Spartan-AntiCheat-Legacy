package com.vagdedes.spartan.checks.combat.killAura;

// Study of the data that has been collected

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.objects.profiling.PlayerFight;

public class Analysis extends DetectionExecutor {

    Analysis(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(PlayerFight fight) {
        double percentage = hackType.getCheck().getDecimalOption("analysis_percentage", 2.0);

        if (percentage > 0.0) {
            if (percentage > 5.0) {
                percentage = 5.0;
            }
            forceAsync(() -> {
                // direction change
                // directions difference
                // clicks per second
                // hit-damage ratio
                // hit-enemy-hit ratio
                // hit reach
                // hit time
                // hit combo
                // fight duration
            });
        }
    }
}
