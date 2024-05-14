package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

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
                        int limit = PlayerUtils.getJumpTicks(0);

                        if (airTicks > limit) {
                            cancel(
                                    "type: horizontal(known)"
                                            + ", box: " + box
                                            + ", old-box: " + oldBox
                                            + ", vertical: " + vertical
                                            + ", air-ticks: " + airTicks
                                            + ", limit: " + limit,
                                    1.0,
                                    player.movement.getDetectionLocation(),
                                    0,
                                    true
                            );
                        }
                    } else {
                        cancel(
                                "type: horizontal(abstract)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical
                                        + ", air-ticks: " + airTicks,
                                1.0,
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
