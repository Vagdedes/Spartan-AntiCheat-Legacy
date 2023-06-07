package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class IrregularMovementsLiquids {

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                    double vertical, double horizontal,
                    boolean pastFalling, boolean liquidBelow, boolean knownBoxes, boolean building, boolean jumping, boolean oldJumping, boolean changedScenario,
                    String detection, String prevention) {
        if (IrregularMovements.check.getCheck().getBooleanOption("check_liquids", true)) {
            AsyncDetection.run(p, IrregularMovements.check, () -> {
                if (liquidBelow) {
                    if (!building
                            && BlockUtils.isLiquid(to.clone().add(0, -1, 0))

                            && BlockUtils.isSolidFree(to, BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max)
                            && BlockUtils.isSolidFree(from, BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max)

                            && BlockUtils.isNonSolidFree(to, BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max)
                            && BlockUtils.isNonSolidFree(from, BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max)

                            && BlockUtils.isWaterLogFree(to, BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max)
                            && BlockUtils.isWaterLogFree(from, BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max)

                            && BlockUtils.isNonSolidFree(to, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                            && BlockUtils.isNonSolidFree(from, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)

                            && BlockUtils.isWaterLogFree(to, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                            && BlockUtils.isWaterLogFree(from, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)) {
                        if (jumping) {
                            IrregularMovements.cancel(p, from, prevention, "jumping-" + detection, null, oldJumping ? 3 : 2, -IrregularMovements.defaultBufferTime);
                        } else {
                            double requiredHorizontal = 0.15;

                            if (Math.abs(vertical) < 0.01 && horizontal >= requiredHorizontal) {
                                IrregularMovements.cancel(p, from, prevention, "horizontal-" + detection, 3);
                            } else {
                                double limit = p.getLastLiquidTime() <= 500L ? 0.4 : 0.2;

                                if (vertical >= limit) {
                                    IrregularMovements.cancel(p, from, prevention, "abstract-up-" + detection, "limit: " + limit, 3, -IrregularMovements.defaultBufferTime);
                                } else if (horizontal >= requiredHorizontal) {
                                    IrregularMovements.cancel(p, from, prevention, "consistency-" + AlgebraUtils.cut(vertical, 3) + "-" + detection, null, 3, -IrregularMovements.defaultBufferTime);
                                }
                            }
                        }
                    }
                } else {
                    double defaultThreshold = 0.4;

                    if (p.isSwimming()) {
                        double threshold = knownBoxes ? defaultThreshold * 2.0 : defaultThreshold;

                        if (vertical >= threshold) {
                            IrregularMovements.cancel(p, from, prevention, "swimming-up-" + detection, "limit: " + threshold, 3, IrregularMovements.defaultBufferTime);
                        }
                    } else if (vertical >= defaultThreshold) {
                        IrregularMovements.cancel(p, from, prevention, "normal-up-" + detection, 3);
                    }
                    double downwardLimit = pastFalling ? 2.0 : 0.4;

                    // Attention: changedScenario added recently
                    if (!changedScenario && vertical <= -downwardLimit) {
                        IrregularMovements.cancel(p, from,
                                prevention, "down-" + detection, "limit: " + downwardLimit, 3, IrregularMovements.defaultBufferTime);
                    }
                    if (!knownBoxes && vertical > 0.0 && (vertical >= defaultThreshold || AlgebraUtils.isExponentVisible(vertical))) {
                        Timer timer = p.getTimer(IrregularMovements.check);
                        double cutVertical = AlgebraUtils.cut(vertical, 5);
                        String key = detection + "=" + cutVertical;
                        long ms = timer.get(key);
                        timer.set(key);

                        if (ms <= 50L && !IrregularMovements.cancel(p, from, prevention, "repeated-" + detection, 4)) {
                            IrregularMovements.cancel(p, from, prevention, "repeated-" + detection + "=" + cutVertical, 3);
                        }
                    }
                }
            });
        }
    }
}
