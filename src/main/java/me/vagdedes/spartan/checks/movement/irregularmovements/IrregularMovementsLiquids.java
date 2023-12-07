package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class IrregularMovementsLiquids extends DetectionExecutor {

    private final IrregularMovements irregularMovements;

    IrregularMovementsLiquids(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        irregularMovements = (IrregularMovements) executor;
    }

    private static final double[]
            minus = new double[]{
            BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max,
    }, current = new double[]{
            BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max,
    };

    void run(boolean pastFalling, boolean liquidBelow, boolean knownBoxes,
             boolean building, boolean jumping, boolean oldJumping, boolean changedScenario,
             String detection, String prevention) {
        if (hackType.getCheck().getBooleanOption("check_liquids", true)
                && !player.getHandlers().has(Handlers.HandlerType.Vehicle)) {
            Double vertical = player.getNmsVerticalDistance(),
                    horizontal = player.getNmsHorizontalDistance();

            if (vertical == null
                    || horizontal == null) {
                return;
            }
            SpartanLocation to = player.getEventToLocation(),
                    from = player.getEventFromLocation();

            runAsync(() -> {
                if (liquidBelow) {
                    if (!building
                            && BlockUtils.isLiquid(to.clone().add(0, -1, 0))

                            && !PatternUtils.isBlockPattern(minus, to, true, BlockUtils.solid, BlockUtils.semi_solid)
                            && !PatternUtils.isBlockPattern(minus, from, true, BlockUtils.solid, BlockUtils.semi_solid)

                            && !PatternUtils.isBlockPattern(current, to, true, BlockUtils.semi_solid)
                            && !PatternUtils.isBlockPattern(current, from, true, BlockUtils.semi_solid)

                            && !PatternUtils.isWaterLogPattern(minus, to, true)
                            && !PatternUtils.isBlockPattern(minus, from, true)

                            && !PatternUtils.isWaterLogPattern(current, to, true)
                            && !PatternUtils.isBlockPattern(current, from, true)) {
                        if (jumping) {
                            irregularMovements.cancel(prevention, "jumping-" + detection, null, oldJumping ? 3 : 2, -IrregularMovements.defaultBufferTime);
                        } else {
                            double requiredHorizontal = 0.15;

                            if (Math.abs(vertical) < 0.01 && horizontal >= requiredHorizontal) {
                                irregularMovements.cancel(prevention, "horizontal-" + detection, 3);
                            } else {
                                double limit = player.wasInLiquids() ? 0.4 : 0.2;

                                if (vertical >= limit) {
                                    irregularMovements.cancel(prevention, "abstract-up-" + detection, "limit: " + limit, 3, -IrregularMovements.defaultBufferTime);
                                } else if (horizontal >= requiredHorizontal) {
                                    irregularMovements.cancel(prevention, "consistency-" + AlgebraUtils.cut(vertical, 3) + "-" + detection, null, 3, -IrregularMovements.defaultBufferTime);
                                }
                            }
                        }
                    }
                } else {
                    double defaultThreshold = 0.4;

                    if (player.isSwimming()) {
                        double threshold = knownBoxes ? defaultThreshold * 2.0 : defaultThreshold;

                        if (vertical >= threshold) {
                            irregularMovements.cancel(prevention, "swimming-up-" + detection, "limit: " + threshold, 3, IrregularMovements.defaultBufferTime);
                        }
                    } else if (vertical >= defaultThreshold) {
                        irregularMovements.cancel(prevention, "normal-up-" + detection, 3);
                    }
                    double downwardLimit = pastFalling ? 2.0 : 0.4;

                    // Attention: changedScenario added recently
                    if (!changedScenario && vertical <= -downwardLimit) {
                        irregularMovements.cancel(prevention, "down-" + detection, "limit: " + downwardLimit, 3, IrregularMovements.defaultBufferTime);
                    }
                    if (!knownBoxes && vertical > 0.0 && (vertical >= defaultThreshold || AlgebraUtils.isExponentVisible(vertical))) {
                        Timer timer = player.getTimer(hackType);
                        double cutVertical = AlgebraUtils.cut(vertical, 5);
                        String key = detection + "=" + cutVertical;
                        long ms = timer.get(key);
                        timer.set(key);

                        if (ms <= 50L && !irregularMovements.cancel(prevention, "repeated-" + detection, 4)) {
                            irregularMovements.cancel(prevention, "repeated-" + detection + "=" + cutVertical, 3);
                        }
                    }
                }
            });
        }
    }
}
