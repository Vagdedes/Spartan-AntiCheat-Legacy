package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class IrregularMovementsStep extends DetectionExecutor {

    IrregularMovementsStep(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (hackType.getCheck().getBooleanOption("check_step", true)) {
            runAsync(() -> {
                player.debug(false, false, true,
                        1
                );
            });
        }
    }
}
