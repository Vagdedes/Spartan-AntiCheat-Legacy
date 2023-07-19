package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class IrregularMovementsMotion {

    private static final double
            postJumpPreFallPrecision = MoveUtils.jumping[2] * 7, // 0.0007
            criticalStepLimit = 0.5 * Math.round((GroundUtils.maxPlayerStep * 2.0) / 0.5); // 1.5
    private static final int criticalTickDifference = MoveUtils.jumpingMotions * 3; // 15

    private static final String
            upwardsProgressiveKey = "upwards-progressive",
            downwardsDifferenceKey = "downwards-difference",
            downwardsPredictionKey = "downwards-prediction",
            downwardsUnexpectedKey = "downwards-unexpected";

    private static void clear(SpartanPlayer p, Decimals decimals) {
        IrregularMovements.clear(p, upwardsProgressiveKey);
        // no Decimals usage

        IrregularMovements.clear(p, downwardsUnexpectedKey);
        // no Decimals usage

        IrregularMovements.clear(p, downwardsDifferenceKey);
        decimals.remove(downwardsDifferenceKey);

        IrregularMovements.clear(p, downwardsPredictionKey);
        decimals.remove(downwardsPredictionKey);
    }

    static void run(SpartanPlayer p,
                    SpartanLocation to, SpartanLocation from,
                    double vertical, double oldVertical, double verticalDifference, double oldVerticalDifference,
                    double box, double oldBox,
                    boolean knownBoxes, boolean knownBoxesNew, boolean knownBoxesOld,
                    boolean onGroundVanilla, boolean onGroundCustom,
                    boolean constructDetection, boolean groundInteractionScenario, boolean velocityScenario,
                    boolean checkUp, boolean checkDown, boolean checkJump, boolean checkFall, boolean checkHorizontal,
                    int airTicks,
                    String prevention) {
        Check check = IrregularMovements.check.getCheck();
        double stepLimit = check.getDecimalOption("step_limit_in_blocks", GroundUtils.maxPlayerStep);
        boolean checkUpASYNC = checkUp && check.getBooleanOption("check_upwards", true),
                checkDownASYNC = checkDown && check.getBooleanOption("check_downwards", true),
                checkJumpASYNC = checkJump && check.getBooleanOption("check_jumping", true),
                checkFallASYNC = checkFall && check.getBooleanOption("check_falling", true),
                checkHorizontalASYNC = checkHorizontal && check.getBooleanOption("check_horizontal", true),
                onGround = onGroundCustom || onGroundVanilla && airTicks == 0;

        // Separator
        AsyncDetection.run(p, IrregularMovements.check, () -> {
            Decimals decimals = p.getDecimals(IrregularMovements.check);

            if (onGround) { // Ground
                clear(p, decimals);
            } else { // Motion
                if (vertical > 0.0) {
                    if (checkUpASYNC
                            && !knownBoxesNew
                            && !p.isJumping(vertical)
                            && Math.abs(vertical - MoveUtils.postJumpPreFall) >= postJumpPreFallPrecision) {
                        boolean abstractScenario = groundInteractionScenario || velocityScenario;

                        if (stepLimit > 0.0) {
                            if (abstractScenario) {
                                //todo fix limit
                            } else if (vertical > stepLimit) {
                                IrregularMovements.cancel(p, from, prevention,
                                        "upwards-instant", "limit: " + stepLimit,
                                        1, 0);
                            }
                        }
                        if (abstractScenario) {
                            // todo finish by checking acceleration
                            p.debug(false, true,
                                    vertical, verticalDifference
                            );
                        } else {
                            IrregularMovements.cancel(p, from, prevention,
                                    upwardsProgressiveKey, null,
                                    5, AlgebraUtils.pow2(IrregularMovements.defaultBufferTime));
                        }
                    }
                } else if ((checkDownASYNC || checkFallASYNC)
                        && !velocityScenario) {
                    if (vertical < 0.0) {
                        int expectedTick = MoveUtils.getFallingTick(vertical);

                        if (constructDetection) {
                            decimals.remove(downwardsDifferenceKey);
                        } else {
                            int fallingToAirTicksDifference = airTicks - p.getFallingTicks(); // Do not consider when air-ticks are less than falling-ticks

                            if (fallingToAirTicksDifference >= criticalTickDifference) {
                                if (decimals.get(downwardsDifferenceKey) != fallingToAirTicksDifference) {
                                    IrregularMovements.cancel(p, from, prevention,
                                            downwardsDifferenceKey, "difference: " + fallingToAirTicksDifference,
                                            5, AlgebraUtils.pow2(IrregularMovements.defaultBufferTime));
                                }
                                decimals.set(downwardsDifferenceKey, fallingToAirTicksDifference);
                            } else {
                                decimals.remove(downwardsDifferenceKey);
                            }
                        }
                        if (expectedTick != -1) {
                            int predictedExpectedTick = AlgebraUtils.integerRound(
                                    decimals.get(downwardsPredictionKey, -1.0)
                            );

                            if (predictedExpectedTick != -1) {
                                int inaccuracy = AlgebraUtils.integerRound(
                                        Math.abs(expectedTick - predictedExpectedTick)
                                );

                                if (inaccuracy > 1) {
                                    IrregularMovements.cancel(p, from,
                                            prevention, downwardsPredictionKey, "inaccuracy: " + inaccuracy,
                                            5, AlgebraUtils.pow2(IrregularMovements.defaultBufferTime));
                                }
                            }
                            decimals.set(
                                    downwardsPredictionKey,
                                    MoveUtils.getFallingTick((vertical * MoveUtils.dragComplement) - MoveUtils.gravityAcceleration)
                            );
                        } else {
                            decimals.remove(downwardsPredictionKey);
                            IrregularMovements.cancel(p, from,
                                    prevention, downwardsUnexpectedKey, null,
                                    5, AlgebraUtils.pow2(IrregularMovements.defaultBufferTime));
                        }
                    }
                } else {
                    clear(p, decimals);
                }
            }

            // Separator

            if (stepLimit > 0.0 && (checkUpASYNC || checkJumpASYNC)) { // Step
                if (knownBoxesNew
                        && knownBoxesOld) { // Step
                    double boxDifference = Math.abs((to.getBlockY() + box) - (oldBox + from.getBlockY()));

                    if (vertical > 0.0) {
                        if (boxDifference > stepLimit) {
                            IrregularMovements.cancel(p, from, prevention, "step-up-predictable",
                                    "box-difference: " + boxDifference + ", limit: " + stepLimit,
                                    !velocityScenario && boxDifference >= criticalStepLimit ? 1 : 2,
                                    velocityScenario ? -IrregularMovements.defaultBufferTime : IrregularMovements.defaultBufferTime);
                        }
                    } else if (vertical < 0.0 //todo false positives
                            && boxDifference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Anything below this threshold is likely instant, where otherwise you fall for a bit
                        IrregularMovements.cancel(p, from, prevention, "step-down-predictable", "box-difference: " + boxDifference, 1, 0);
                    }
                } else if (knownBoxesNew) {
                    if (vertical > stepLimit) {
                        IrregularMovements.cancel(p, from, prevention, "step-up-known", "limit: " + stepLimit,
                                !velocityScenario && vertical >= criticalStepLimit ? 1 : 2,
                                velocityScenario ? -IrregularMovements.defaultBufferTime : IrregularMovements.defaultBufferTime);
                    } else if (vertical < MoveUtils.gravityAcceleration) {
                        //todo step down known
                    }
                }
            }

            if (vertical < oldVertical) { // Fall
                if (p.isJumping(vertical)) {
                    if (checkJumpASYNC
                            && p.isJumping(oldVertical)) {
                        double difference = Math.abs(verticalDifference);

                        if (difference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Jumping motions must not skip each other
                            IrregularMovements.cancel(p, from, prevention, "jump-up-skip", 1);
                        } else if (difference < MoveUtils.highPrecision) { // Jumping motions must not be the same
                            IrregularMovements.cancel(p, from, prevention, "jump-up-equilibrium",
                                    groundInteractionScenario ? MoveUtils.jumpingMotions : 1);
                        }
                    }
                } else if (p.isJumping(oldVertical)) {
                    if (checkJumpASYNC) {
                        double jumpingPrecision = MoveUtils.getJumpingPrecision(p);

                        if (Math.abs(oldVertical - MoveUtils.jumping[0]) >= jumpingPrecision // Jumping must end somewhere
                                && !PatternUtils.isBlockPattern(new double[]{
                                BlockUtils.hitbox_max, 2 - box, BlockUtils.hitbox_max
                        }, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                            IrregularMovements.cancel(p, from, prevention, "jump-end",
                                    groundInteractionScenario || velocityScenario ? MoveUtils.jumpingMotions : 1);
                        }
                    }
                }
            } else if (vertical > oldVertical) { // Ascension
                if (p.isJumping(vertical)) {
                    if (checkJumpASYNC) {
                        if (p.isJumping(oldVertical)) {
                            double difference = Math.abs(verticalDifference);

                            if (difference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Jumping motions must not skip each other
                                IrregularMovements.cancel(p, from, prevention, "jump-down-skip",
                                        groundInteractionScenario ? MoveUtils.jumpingMotions : 1);
                            } else if (difference < MoveUtils.highPrecision) { // Jumping motions must not be the same
                                IrregularMovements.cancel(p, from, prevention, "jump-down-equilibrium",
                                        groundInteractionScenario ? MoveUtils.jumpingMotions : 1);
                            }
                        } else if (!onGroundCustom
                                && !GroundUtils.heightExists(Math.abs(box - vertical), true)
                                && Math.abs(vertical - MoveUtils.jumping[1]) < MoveUtils.getJumpingPrecision(p)) { // Jumping must start from somewhere
                            IrregularMovements.cancel(p, from, prevention, "jump-start", 1);
                        }
                    }
                }
            } else if (checkHorizontalASYNC
                    && !onGround) { // Equilibrium
                double[][] positions = new double[][]{
                        {1, 0, 1},
                        {1, -(box + GroundUtils.minPossibleStep), 1},
                        {1, 1, 1},
                };

                if (!PatternUtils.isBlockPattern(positions, to, true, BlockUtils.solid, BlockUtils.semi_solid)
                        && !PatternUtils.isBlockPattern(positions, from, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                    IrregularMovements.cancel(p, from, prevention, "equilibrium-spacious", 1);
                } else if (!knownBoxes
                        && !GroundUtils.heightExists(oldBox - oldVertical, true)) {
                    IrregularMovements.cancel(p, from, prevention, "*equilibrium-abstract", 2);
                } else {
                    IrregularMovements.cancel(p, from, prevention, "*equilibrium-predictable", 3);
                }
            }
        });
    }
}
