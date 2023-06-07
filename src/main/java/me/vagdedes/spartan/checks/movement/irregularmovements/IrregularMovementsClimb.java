package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;

public class IrregularMovementsClimb {

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                       double vertical, double oldVertical, double box,
                       boolean knownBoxes, boolean canGoUp, boolean canGoDown, boolean canJump, boolean bouncingBlocks, boolean jumping,
                       String prevention, String scenario) {
        if (IrregularMovements.check.getCheck().getBooleanOption("check_climbing", true)) {
            AsyncDetection.run(p, IrregularMovements.check, () -> {
                if (jumping) {
                    if (canJump && !knownBoxes && p.isClimbing(oldVertical)

                            // Attention, added recently
                            && (Math.abs(vertical - MoveUtils.jumping[1]) >= MoveUtils.jumping[2] // Not first jump motion
                            || Math.abs(vertical - box) >= MoveUtils.highPrecision)) { // Vertical & box are not correlated
                        IrregularMovements.cancel(p, from, prevention, "jump-" + scenario, 1);
                    }
                } else if (!p.isClimbing(vertical)) {
                    boolean scaffolding = BlockUtils.isScaffoldingBlock(to)
                            || BlockUtils.isScaffoldingBlock(to.clone().add(0, 1, 0));
                    double max = (scaffolding ? MoveUtils.climbingScaffoldingMax : MoveUtils.getClimbingMotionEdges()[1]) + MoveUtils.lowPrecision;

                    if (canGoUp && vertical >= max) {
                        IrregularMovements.cancel(p, from, prevention, "up-" + scenario, "scaffolding: " + scaffolding + ", limit: " + max, bouncingBlocks || knownBoxes ? 8 : 3, IrregularMovements.defaultBufferTime);
                    } else if (canGoDown && vertical <= -max
                            && !p.isFalling(vertical) && p.getLastFall() >= 1_000L) {
                        boolean nonClimbable = IrregularMovements.cacheBoolean(p,
                                !BlockUtils.canClimb(to)
                                        || !BlockUtils.canClimb(to.clone().add(0, 1, 0))
                                        || !BlockUtils.canClimb(to.clone().add(0, -1, 0)),
                                "climb=non-climbable", 10);
                        IrregularMovements.cancel(p, from, prevention, "down-" + scenario, "scaffolding: " + scaffolding + ", limit: " + max, nonClimbable ? 8 : 3, 10);
                    }
                }
            });
        }
    }
}
