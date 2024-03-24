package com.vagdedes.spartan.checks.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerData;

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
                && !player.getHandlers().has(Handlers.HandlerType.Vehicle)) {
            Double vertical = player.getNmsVerticalDistance(),
                    oldVertical = player.getPreviousNmsVerticalDistance(),
                    box = player.getNmsBox();

            if (vertical == null
                    || oldVertical == null
                    || box == null) {
                return;
            }
            SpartanLocation to = player.getEventToLocation();

            runAsync(() -> {
                if (jumping) {
                    if (canJump && !knownBoxes && player.isClimbing(oldVertical)

                            // Attention, added recently
                            && (Math.abs(vertical - PlayerData.jumping[1]) >= PlayerData.getJumpingPrecision(player) // Not first jump motion
                            || Math.abs(vertical - box) >= PlayerData.highPrecision)) { // Vertical & box are not correlated
                        irregularMovements.cancel(prevention, "jump-" + scenario, 1);
                    }
                } else if (!player.isClimbing(vertical)) {
                    boolean scaffolding = PatternUtils.collides(to, true, true, 0.0, BlockUtils.scaffolding);
                    double jumpEffectExtra = player.getExtraMovementFromJumpEffect(),
                            max = (scaffolding ? PlayerData.climbingScaffoldingMax : PlayerData.climbingMinMax[1]) + (jumpEffectExtra > 0 ? jumpEffectExtra + 0.1 : PlayerData.lowPrecision);

                    if (canGoUp && vertical >= max) {
                        irregularMovements.cancel(prevention, "up-" + scenario, "scaffolding: " + scaffolding + ", limit: " + max, bouncingBlocks || knownBoxes ? 8 : 3, IrregularMovements.defaultBufferTime);
                    } else if (canGoDown && vertical <= -max
                            && !player.isFalling(vertical) && player.getLastFall() >= 1_000L) {
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
