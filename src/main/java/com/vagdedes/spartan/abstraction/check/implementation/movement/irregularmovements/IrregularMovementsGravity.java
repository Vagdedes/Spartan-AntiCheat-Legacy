package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

public class IrregularMovementsGravity extends DetectionExecutor {

    private final IrregularMovements irregularMovements;

    IrregularMovementsGravity(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        irregularMovements = (IrregularMovements) executor;
    }

    void run(double vertical, double oldVertical, double box, double oldBox) {
        if (GroundUtils.blockHeightExists(box) || GroundUtils.blockHeightExists(oldBox)) {
            player.debug(false, false, true,
                    1,
                    box,
                    oldBox,
                    vertical,
                    oldVertical
            );
        } else {
            double prediction = PlayerUtils.calculateNextFallMotion(oldVertical, GroundUtils.maxHeightLengthRatio),
                    difference = Math.abs(vertical - prediction);

            if (difference >= GroundUtils.maxHeightLengthRatio) {
                player.debug(false, false, true,
                        0,
                        difference,
                        box,
                        oldBox,
                        vertical,
                        oldVertical
                );
            }
        }
    }
}
