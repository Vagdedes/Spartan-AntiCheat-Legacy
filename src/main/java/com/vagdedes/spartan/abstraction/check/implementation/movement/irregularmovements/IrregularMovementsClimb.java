package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

public class IrregularMovementsClimb extends DetectionExecutor {

    private final IrregularMovements irregularMovements;

    IrregularMovementsClimb(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        irregularMovements = (IrregularMovements) executor;
    }

    void run(Cooldowns cooldowns,
             boolean knownBoxes, boolean canGoUp, boolean canGoDown,
             boolean canJump, boolean bouncingBlocks, boolean jumping,
             String prevention, String scenario) {
        if (hackType.getCheck().getBooleanOption("check_climbing", true)
                && !player.getTrackers().has(Trackers.TrackerType.Vehicle)) {
            Double vertical = player.movement.getNmsVerticalDistance(),
                    oldVertical = player.movement.getPreviousNmsVerticalDistance(),
                    box = player.movement.getNmsBox();

            if (vertical == null
                    || oldVertical == null
                    || box == null) {
                return;
            }
            SpartanLocation to = player.movement.getNmsToLocation();

            runAsync(() -> {
                if (jumping) {
                    if (canJump && !knownBoxes && player.movement.isClimbing(oldVertical)

                            // Attention, added recently
                            && (Math.abs(vertical - PlayerUtils.getJumpMotion(0)) >= PlayerUtils.getJumpingPrecision(player) // Not first jump motion
                            || Math.abs(vertical - box) >= PlayerUtils.highPrecision)) { // Vertical & box are not correlated
                        irregularMovements.cancel(prevention, "jump-" + scenario, 1);
                    }
                } else if (!player.movement.isClimbing(vertical)) {
                    boolean scaffolding = PatternUtils.collides(to, true, true, 0.0, BlockUtils.scaffolding);
                    double jumpEffectExtra = player.movement.getExtraMovementFromJumpEffect(),
                            max = (scaffolding ? PlayerUtils.climbingScaffoldingMax : PlayerUtils.climbingMinMax[1]) + (jumpEffectExtra > 0 ? jumpEffectExtra + 0.1 : PlayerUtils.lowPrecision);

                    if (canGoUp && vertical >= max) {
                        irregularMovements.cancel(prevention, "up-" + scenario, "scaffolding: " + scaffolding + ", limit: " + max, bouncingBlocks || knownBoxes ? 8 : 3, IrregularMovements.defaultBufferTime);
                    } else if (canGoDown && vertical <= -max
                            && !PlayerUtils.isFalling(vertical) && player.movement.getLastFall() >= 1_000L) {
                        boolean nonClimbable = irregularMovements.cacheBoolean(cooldowns,
                                !BlockUtils.canClimb(to)
                                        || !BlockUtils.canClimb(to.clone().add(0, 1, 0))
                                        || !BlockUtils.canClimb(to.clone().add(0, -1, 0)),
                                "climb=non-climbable", 10);
                        irregularMovements.cancel(prevention, "down-" + scenario, "scaffolding: " + scaffolding + ", limit: " + max, nonClimbable ? 8 : 3, 10);
                    }
                }
            });
        }
    }
}
