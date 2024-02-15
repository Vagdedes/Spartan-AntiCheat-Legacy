package com.vagdedes.spartan.checks.combat.killAura;

// Study of the data that has been collected

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.profiling.PlayerFight;
import com.vagdedes.spartan.objects.profiling.PlayerOpponent;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Analysis extends DetectionExecutor {

    Analysis(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(PlayerFight fight) {
        if (!ResearchEngine.isCaching()) {
            double percentage = hackType.getCheck().getDecimalOption("analysis_percentage", 2.0);

            if (percentage > 0.0) {
                if (percentage > 5.0) {
                    percentage = 5.0;
                }
                forceAsync(() -> {
                    //todo
                    // direction change
                    // directions difference
                    // clicks per second
                    // hit-damage ratio
                    // hit reach
                    // hit time
                    // hit combo
                    // fight duration
                });
            }
        }
    }
}
