package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class IrregularMovementsHorizontal extends DetectionExecutor {

    IrregularMovementsHorizontal(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (hackType.getCheck().getBooleanOption("check_horizontal", true)) {
            runAsync(() -> {
                // todo
            });
        }
    }
}
