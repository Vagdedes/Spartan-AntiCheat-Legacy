package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;

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
                            && (Math.abs(vertical - MoveUtils.jumping[1]) >= MoveUtils.getJumpingPrecision(player) // Not first jump motion
                            || Math.abs(vertical - box) >= MoveUtils.highPrecision)) { // Vertical & box are not correlated
                        irregularMovements.cancel(prevention, "jump-" + scenario, 1);
                    }
                } else if (!player.isClimbing(vertical)) {
                    boolean scaffolding = PatternUtils.collides(to, true, true, 0.0, BlockUtils.scaffolding);
                    double jumpEffectExtra = player.getExtraMovementFromJumpEffect(),
                            max = (scaffolding ? MoveUtils.climbingScaffoldingMax : MoveUtils.getClimbingMotionEdges()[1]) + (jumpEffectExtra > 0 ? jumpEffectExtra + 0.1 : MoveUtils.lowPrecision);

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
