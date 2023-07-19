package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class IrregularMovementsCobweb {

    private static double[][] locations = new double[][]{
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max}
    };

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                    double vertical, double horizontal, double box, double oldBox,
                    boolean canGoDown,
                    String prevention, String scenario) {
        if (IrregularMovements.check.getCheck().getBooleanOption("check_cobweb", true)) {
            AsyncDetection.run(p, IrregularMovements.check, () -> {
                if (vertical == 0.0) {
                    if (horizontal >= 0.01 && !p.isOnGround()
                            || !PatternUtils.isBlockPattern(locations, to, true, BlockUtils.solid, BlockUtils.semi_solid)) { // Second is used in case the first method is spoofed
                        double selectedBox = AlgebraUtils.cut(GroundUtils.heightExists(box, true) ? box : oldBox, 5);
                        IrregularMovements.cancel(p, from, prevention,
                                "horizontal-" + scenario + "=" + selectedBox, "selected-box: " + selectedBox,
                                6, IrregularMovements.defaultBufferTime);
                    }
                } else {
                    if (canGoDown) {
                        long lastFall = p.getLastFall();

                        if (vertical <= -(lastFall <= 105L ? 4.0 : lastFall <= 205L ? 0.2 : 0.1)) { // Last fall is checked to modify the limit and prevent fast falling false positives
                            IrregularMovements.cancel(p, from, prevention, "down-" + scenario, 2);
                        }
                    } // Do not create an up detection as it is covered by the illegal-up algorithm
                }
            });
        }
    }
}
