package me.vagdedes.spartan.checks.combat.killAura;

// Study of the data that has been collected

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;

public class Analysis extends DetectionExecutor {

    Analysis(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (!ResearchEngine.isCaching()) {
            int percentage = hackType.getCheck().getNumericalOption("analysis_percentage", 25);

            if (percentage > 0 && !ResearchEngine.getFights().isEmpty()) {
                if (percentage > 1000) {
                    percentage = 1000;
                }
                runAsync(() -> {
                    //todo
                });
            }
        }
    }
}
