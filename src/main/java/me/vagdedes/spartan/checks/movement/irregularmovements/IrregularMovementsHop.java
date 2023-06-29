package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.features.protections.Building;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class IrregularMovementsHop {

    private static final double
            verticalAboveZeroMotion = 0.082,
            verticalBelowZeroMotion = 0.075,
            redundantMotion = 0.001335979112141672;

    private static final double[][]
            locations1 = new double[][]{
            {1, -2, 1},
            {1, -1, 1},
            {1, 0, 1},
            {1, 1, 1},
            {1, 2, 1},
    }, locations2 = new double[][]{
            {1, 0, 1},
            {1, 1, 1},
            {1, 2, 1}
    };

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from, Cooldowns cooldowns, Timer timer,
                    double vertical, double oldVertical, double box, double oldBox, double horizontal,
                    boolean jumping, boolean oldJumping,
                    String prevention) {
        if (IrregularMovements.check.getCheck().getBooleanOption("check_hopping", true)) {
            AsyncDetection.run(p, IrregularMovements.check, () -> {
                if (vertical > -2.0 && to.getBlockY() > 0 // Position
                        && box != 0.0 && box != oldBox // Box
                        && !jumping // Motion
                        && !PlayerData.hasSpeedEffect(p, to) // Effect
                        && !p.getHandlers().has(Handlers.HandlerType.ElytraWear) // State
                        && !BlockUtils.isAnvilBugged(to) && !p.stepsOnBoats() && !Building.hasCooldown(p) // Situation

                        // Environment
                        && !BlockUtils.areCobbleWalls(to.clone().add(0, -1, 0))
                        && BlockUtils.isCoralFanFree(to, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                        && BlockUtils.isInteractiveBushFree(to, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                        && BlockUtils.isHoneyBlockFree(to, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                        && BlockUtils.isHoneyBlockFree(to, BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max)) {
                    if (PatternUtils.isBlockPattern(locations1, to, true, false, BlockUtils.semi_solid)) {
                        cooldowns.add("hop=non-solid-free", 20);
                        return;
                    }
                    if (!cooldowns.canDo("hop=non-solid-free")) {
                        return;
                    }
                    boolean solidFree = !PatternUtils.isBlockPattern(locations2, to, true, false, BlockUtils.solid);

                    if (BlockUtils.isSolidFree(to, 1, 2, 1)) {
                        boolean staticB, dynamic,
                                verticalAboveZero = vertical > 0.0;
                        double limit = verticalAboveZero ? verticalAboveZeroMotion : verticalBelowZeroMotion,
                                difference = Math.abs(box - oldBox);

                        if (verticalAboveZero || vertical == 0.0) {
                            staticB = box >= 0.05 && box <= verticalAboveZeroMotion;
                            dynamic = box >= 0.05 && difference <= verticalAboveZeroMotion;

                            if (verticalAboveZero) {
                                timer.set("hop-movement=delay");
                            }
                            if (solidFree && !oldJumping
                                    && (p.isOnGround() || p.isOnGroundCustom() || PlayerData.isOnGround(p, to, 0, -1, 0))) {
                                long ms = timer.get("hop-movement=jump");

                                if (ms <= 350L) {
                                    IrregularMovements.cancel(p, from, prevention, "hop-jump-fast", 2); // Attention, recently changed the buffer from 1 to 2
                                } else if (ms <= 550L && oldVertical <= -0.07) {
                                    IrregularMovements.cancel(p, from, prevention, "hop-jump-slow", "ms: " + ms + ", old-vertical: " + oldVertical, 2, -IrregularMovements.defaultBufferTime);
                                }
                                timer.set("hop-movement=jump");
                            }
                        } else {
                            staticB = box <= verticalBelowZeroMotion;
                            dynamic = difference <= verticalBelowZeroMotion;

                            if (horizontal >= 0.1) {
                                long ms = timer.get("hop-movement=delay");

                                if (ms <= 150L && difference <= limit && AlgebraUtils.cut(box, 4) != 0.7706) { // Let it be dynamic
                                    IrregularMovements.cancel(p, from, prevention, "hop-frequent", null, 2, -IrregularMovements.defaultBufferTime);
                                }
                            }
                        }

                        if (staticB && box >= 0.003) {
                            IrregularMovements.cancel(p, from, prevention, "hop-static", "box: " + box, 8, -10);
                        }

                        if (dynamic && difference != 0.0) {
                            IrregularMovements.cancel(p, from, prevention, "hop-dynamic", "difference: " + difference, 5, -10);
                        }

                        if (box <= limit) {
                            if (box < 0.01 && Math.abs(box - redundantMotion) >= MoveUtils.jumping[2]) {
                                IrregularMovements.cancel(p, from, prevention, "hop-micro", "box: " + box, 2, -IrregularMovements.defaultBufferTime);
                            }

                            if (solidFree) {
                                double boxCut = AlgebraUtils.cut(box, 5);
                                long ms = timer.get("hop=repeated=" + boxCut);

                                if (ms <= 400L) {
                                    IrregularMovements.cancel(p, from, prevention, "hop-repeated", "ms: " + ms, 3, -IrregularMovements.defaultBufferTime);
                                }
                                timer.set("hop=repeated=" + boxCut);
                            } else {
                                timer.clear("hop=repeated=");
                            }
                        }
                    }
                }
            });
        }
    }

}
