package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class IrregularMovementsHorizontal extends DetectionExecutor {

    IrregularMovementsHorizontal(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(boolean knownBox, boolean knownOldBox, double box, double oldBox, double vertical) {
        if (hackType.getCheck().getBooleanOption("check_horizontal", true)) {
            int airTicks = player.movement.getTicksOnAir();

            runAsync(() -> {
                if (airTicks > 0) {
                    if (knownBox || knownOldBox) {
                        player.debug(true, false, false,
                                player.movement.getTicksOnAir(), box, oldBox
                        );
                    } else {
                        cancel(
                                "type: horizontal(abstract)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical
                                        + ", air-ticks: " + airTicks,
                                player.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                }
            });
        }
    }
}
