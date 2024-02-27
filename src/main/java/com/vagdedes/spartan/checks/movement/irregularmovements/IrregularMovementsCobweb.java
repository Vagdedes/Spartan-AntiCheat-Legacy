package com.vagdedes.spartan.checks.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class IrregularMovementsCobweb extends DetectionExecutor {

    private final IrregularMovements irregularMovements;

    IrregularMovementsCobweb(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        irregularMovements = (IrregularMovements) executor;
    }

    private static final double[][] locations = new double[][]{
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max}
    };

    void run(boolean canGoDown, String prevention, String scenario) {
        if (hackType.getCheck().getBooleanOption("check_cobweb", true)) {
            Double vertical = player.getNmsVerticalDistance(),
                    horizontal = player.getNmsHorizontalDistance(),
                    box = player.getNmsBox(),
                    oldBox = player.getPreviousNmsBox();

            if (vertical == null
                    || horizontal == null
                    || box == null
                    || oldBox == null) {
                return;
            }
            SpartanLocation to = player.getEventToLocation();
            boolean vehicle = player.getHandlers().has(Handlers.HandlerType.Vehicle);

            runAsync(() -> {
                if (vertical == 0.0) {
                    if (horizontal >= (vehicle ? 0.2 : 0.01) && !player.isOnGround()
                            || !vehicle && !PatternUtils.isBlockPattern(locations, to, true, BlockUtils.solid, BlockUtils.semi_solid)) { // Second is used in case the first method is spoofed
                        double selectedBox = AlgebraUtils.cut(GroundUtils.heightExists(box) ? box : oldBox, 5);
                        irregularMovements.cancel(prevention,
                                "horizontal-" + scenario + "=" + selectedBox, "selected-box: " + selectedBox,
                                6, IrregularMovements.defaultBufferTime);
                    }
                } else if (!vehicle && canGoDown) {
                    long lastFall = player.getLastFall();

                    if (vertical <= -(lastFall <= 105L ? 4.0 : lastFall <= 205L ? 0.2 : 0.1)) { // Last fall is checked to modify the limit and prevent fast falling false positives
                        irregularMovements.cancel(prevention, "down-" + scenario, 2);
                    }
                } // Do not create an up detection as it is covered by the illegal-up algorithm
            });
        }
    }
}
