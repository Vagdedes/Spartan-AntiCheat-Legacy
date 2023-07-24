package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.utils.gameplay.*;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class IrregularMovementsMotion {

    private static final double
            criticalDecelerationLimit = -0.1,
            criticalUpwardsLimit = Math.sqrt(MoveUtils.chunk), // 4.0
            postJumpPreFallPrecision = MoveUtils.jumping[2] * 7, // 0.0007
            criticalStepLimit = GroundUtils.maxPlayerStep * 2.0; // 1.325
    private static final int
            criticalHoppingBuffer = AlgebraUtils.integerRound(MoveUtils.jumpingMotions / 2.0), // 3.0
            extendedDefaultBufferTime = AlgebraUtils.pow2(IrregularMovements.defaultBufferTime), // 400
            decreasedDefaultBufferTime = AlgebraUtils.integerRound(IrregularMovements.defaultBufferTime / 2.0), // 10
            criticalTickDifference = MoveUtils.jumpingMotions * 4; // 20

    private static final String
            motionJumpKey = "motion-jump-effect",
            motionVelocityKey = "motion-velocity",
            upwardsProgressiveKey = "upwards-progressive",
            upwardsAccelerationKey = "upwards-acceleration",
            upwardsDecelerationKey = "upwards-deceleration",
            downwardsDifferenceKey = "downwards-difference",
            downwardsPredictionKey = "downwards-prediction",
            downwardsUnexpectedKey = "downwards-unexpected";

    private static void clearGeneralCache(Cooldowns cooldowns) {
        IrregularMovements.forgetBoolean(cooldowns, motionJumpKey);
        IrregularMovements.forgetBoolean(cooldowns, motionVelocityKey);
    }

    private static void clearDetectionCache(SpartanPlayer p, Decimals decimals) {
        IrregularMovements.clear(p, upwardsDecelerationKey);
        // no Decimals usage

        IrregularMovements.clear(p, upwardsAccelerationKey);
        // no Decimals usage

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
                    Cooldowns cooldowns,
                    SpartanLocation to, SpartanLocation from,
                    double vertical, double oldVertical, double verticalDifference,
                    double horizontalDifference,
                    double box, double oldBox,
                    boolean knownBoxes, boolean knownBoxesNew, boolean knownBoxesOld,
                    boolean onGroundCustom,
                    boolean oldFalling, boolean levitationEffect,
                    boolean constructDetection, boolean groundInteractionScenario, boolean velocityScenario, boolean changedScenario, boolean climbScenario, boolean slowFallingScenario,
                    boolean checkUp, boolean checkDown, boolean checkJump, boolean checkFall, boolean checkHorizontal,
                    int airTicks,
                    String prevention) {
        Check check = IrregularMovements.check.getCheck();
        double stepLimit = check.getDecimalOption("step_limit_in_blocks", GroundUtils.maxPlayerStep);
        boolean jumpEffect = PlayerData.hasJumpEffect(p),
                checkHoppingASYNC = (checkUp || checkJump) && check.getBooleanOption("check_hopping", true),
                checkUpASYNC = checkUp && check.getBooleanOption("check_upwards", true),
                checkDownASYNC = checkDown && check.getBooleanOption("check_downwards", true),
                checkJumpASYNC = checkJump && check.getBooleanOption("check_jumping", true),
                checkFallASYNC = checkFall && check.getBooleanOption("check_falling", true),
                checkHorizontalASYNC = checkHorizontal && check.getBooleanOption("check_horizontal", true),
                onGround = onGroundCustom || airTicks == 0;

        // Separator
        AsyncDetection.run(p, IrregularMovements.check, () -> {
            Decimals decimals = p.getDecimals(IrregularMovements.check);
            double criticalDecelerationLimit;
            boolean jumpEffectASYNC, velocityScenarioASYNC;

            if (onGround) { // Ground
                clearGeneralCache(cooldowns);
                clearDetectionCache(p, decimals);
                jumpEffectASYNC = jumpEffect;
                velocityScenarioASYNC = velocityScenario;
                criticalDecelerationLimit = jumpEffectASYNC ?
                        (IrregularMovementsMotion.criticalDecelerationLimit * 3) :
                        IrregularMovementsMotion.criticalDecelerationLimit;
            } else { // Motion
                jumpEffectASYNC = IrregularMovements.cacheBoolean(cooldowns, jumpEffect, motionJumpKey, extendedDefaultBufferTime);
                velocityScenarioASYNC = IrregularMovements.cacheBoolean(cooldowns, velocityScenario, motionVelocityKey, extendedDefaultBufferTime);
                criticalDecelerationLimit = jumpEffectASYNC ?
                        (IrregularMovementsMotion.criticalDecelerationLimit * 3) :
                        IrregularMovementsMotion.criticalDecelerationLimit;

                if (vertical > 0.0) {
                    if (checkUpASYNC
                            && !knownBoxesNew
                            && !p.isJumping(vertical)
                            && Math.abs(vertical - MoveUtils.postJumpPreFall) >= postJumpPreFallPrecision) {
                        boolean abstractScenario = groundInteractionScenario || velocityScenarioASYNC;

                        if (stepLimit > 0.0) {
                            double upwardsLimit = Math.max(GroundUtils.maxPlayerStep, stepLimit);

                            if (jumpEffect) {
                                upwardsLimit += p.getExtraMovementFromJumpEffect();
                            }
                            if (abstractScenario) {
                                upwardsLimit = Math.max(upwardsLimit, criticalUpwardsLimit);
                            }
                            if (vertical > upwardsLimit) {
                                IrregularMovements.cancel(p, from, prevention,
                                        "upwards-instant", "limit: " + upwardsLimit,
                                        1, 0);
                            }
                        }
                        if (verticalDifference <= criticalDecelerationLimit) {
                            IrregularMovements.cancel(p, from, prevention,
                                    upwardsDecelerationKey, null,
                                    5, extendedDefaultBufferTime);
                        }
                        if (jumpEffectASYNC || abstractScenario) {
                            if (verticalDifference >= vertical) {
                                IrregularMovements.cancel(p, from, prevention,
                                        upwardsAccelerationKey, null,
                                        5, extendedDefaultBufferTime);
                            }
                        } else {
                            IrregularMovements.cancel(p, from, prevention,
                                    upwardsProgressiveKey, null,
                                    5, extendedDefaultBufferTime);
                        }
                    }
                } else if ((checkDownASYNC || checkFallASYNC)
                        && !slowFallingScenario
                        && !jumpEffectASYNC
                        && !climbScenario) {
                    if (vertical < 0.0) {
                        if (constructDetection || velocityScenarioASYNC) {
                            decimals.remove(downwardsDifferenceKey);
                        } else {
                            int fallingToAirTicksDifference = airTicks - p.getFallingTicks(); // Do not consider when air-ticks are less than falling-ticks

                            if (fallingToAirTicksDifference >= criticalTickDifference) {
                                if (decimals.get(downwardsDifferenceKey) != fallingToAirTicksDifference) {
                                    IrregularMovements.cancel(p, from, prevention,
                                            downwardsDifferenceKey, "difference: " + fallingToAirTicksDifference,
                                            5, extendedDefaultBufferTime);
                                }
                                decimals.set(downwardsDifferenceKey, fallingToAirTicksDifference);
                            } else {
                                decimals.remove(downwardsDifferenceKey);
                            }
                        }
                        int expectedTick = MoveUtils.getFallingTick(vertical);

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
                                            5, extendedDefaultBufferTime);
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
                                    5, extendedDefaultBufferTime);
                        }
                    }
                } else {
                    clearDetectionCache(p, decimals);
                }
            }

            // Separator

            if (checkHoppingASYNC
                    && !constructDetection
                    && !velocityScenarioASYNC
                    && !jumpEffectASYNC
                    && p.isJumping(vertical)) {
                if (p.isVanillaSprinting()) {
                    if (Math.abs(vertical - MoveUtils.jumping[1]) < MoveUtils.getJumpingPrecision(p)) {
                        if (horizontalDifference < 0.0) {
                            IrregularMovements.cancel(p, from,
                                    prevention, "hopping-sprinting-deceleration", null,
                                    criticalHoppingBuffer, decreasedDefaultBufferTime);
                        }
                    } else if (horizontalDifference >= 0.03) { // Normally, is anything greater than 0.0 but this is more stable
                        IrregularMovements.cancel(p, from,
                                prevention, "hopping-sprinting-acceleration", null,
                                criticalHoppingBuffer, decreasedDefaultBufferTime);
                    }
                } else {
                    if (Math.abs(vertical - MoveUtils.jumping[1]) < MoveUtils.getJumpingPrecision(p)) {
                        if (horizontalDifference < 0.0) {
                            IrregularMovements.cancel(p, from,
                                    prevention, "hopping-casual-deceleration", null,
                                    criticalHoppingBuffer, decreasedDefaultBufferTime);
                        }
                    }
                }
            }

            // Separator

            if (stepLimit > 0.0
                    && (checkUpASYNC || checkJumpASYNC)) { // Step
                double stepLimitModified = Math.max(GroundUtils.maxPlayerStep, stepLimit);

                if (knownBoxesNew
                        && knownBoxesOld) { // Step
                    double boxDifference = Math.abs((to.getBlockY() + box) - (oldBox + from.getBlockY()));

                    if (vertical > 0.0) {
                        if (boxDifference > stepLimitModified) {
                            IrregularMovements.cancel(p, from, prevention, "step-up-predictable",
                                    "box-difference: " + boxDifference + ", limit: " + stepLimitModified,
                                    !velocityScenarioASYNC && boxDifference >= criticalStepLimit ? 1 : 2,
                                    velocityScenarioASYNC ? -IrregularMovements.defaultBufferTime : IrregularMovements.defaultBufferTime);
                        }
                    } else if (vertical < 0.0
                            && boxDifference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Anything below this threshold is likely instant, where otherwise you fall for a bit
                        IrregularMovements.cancel(p, from, prevention, "step-down-predictable", "box-difference: " + boxDifference, 1, 0);
                    }
                } else if (knownBoxesNew) {
                    if (vertical > stepLimitModified) {
                        IrregularMovements.cancel(p, from, prevention, "step-up-known", "limit: " + stepLimitModified,
                                !velocityScenarioASYNC && vertical >= criticalStepLimit ? 1 : 2,
                                velocityScenarioASYNC ? -IrregularMovements.defaultBufferTime : IrregularMovements.defaultBufferTime);
                    } else if (!oldFalling
                            && !velocityScenarioASYNC
                            && !jumpEffectASYNC
                            && vertical <= criticalDecelerationLimit) {
                        IrregularMovements.cancel(p, from, prevention, "step-down-known", "limit: " + stepLimitModified,
                                vertical <= -criticalStepLimit ? 1 : 2, IrregularMovements.defaultBufferTime);
                    }
                }
            }

            if (vertical < oldVertical) { // Fall
                if (checkJumpASYNC) {
                    if (p.isJumping(vertical)) {
                        if (p.isJumping(oldVertical)) {
                            double difference = Math.abs(verticalDifference);

                            if (difference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Jumping motions must not skip each other
                                IrregularMovements.cancel(p, from, prevention, "jump-up-skip", 1);
                            } else if (difference < MoveUtils.highPrecision) { // Jumping motions must not be the same
                                IrregularMovements.cancel(p, from, prevention, "jump-up-equilibrium",
                                        groundInteractionScenario ? MoveUtils.jumpingMotions : 1);
                            }
                        }
                    } else if (!jumpEffect
                            && !onGround
                            && p.isJumping(oldVertical)) {
                        double jumpingPrecision = MoveUtils.getJumpingPrecision(p);

                        if (Math.abs(oldVertical - MoveUtils.jumping[0]) >= jumpingPrecision // Jumping must end somewhere
                                && !PatternUtils.isBlockPattern(new double[][]{
                                {1, 2 - box, 1},
                                {1, 2 - oldBox, 1},
                                {1, 1, 1},
                                {1, 2, 1}
                        }, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                            IrregularMovements.cancel(p, from, prevention, "jump-end",
                                    groundInteractionScenario || velocityScenarioASYNC ? MoveUtils.jumpingMotions : 1);
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
                                && !GroundUtils.heightExists(Math.abs(box - vertical))
                                && Math.abs(vertical - MoveUtils.jumping[1]) < MoveUtils.getJumpingPrecision(p)) { // Jumping must start from somewhere
                            IrregularMovements.cancel(p, from, prevention, "jump-start", 1);
                        }
                    }
                }
            } else if (checkHorizontalASYNC
                    && !changedScenario
                    && !climbScenario
                    && !onGround) { // Equilibrium
                double[][] positions = new double[][]{
                        {1, 0, 1},
                        {1, -(box + GroundUtils.minPossibleStep), 1},
                        {1, 1, 1},
                };

                if (levitationEffect) {
                    IrregularMovements.cancel(p, from, prevention, "equilibrium-levitation", MoveUtils.jumpingMotions);
                } else if (!PatternUtils.isBlockPattern(positions, to, true, BlockUtils.solid, BlockUtils.semi_solid)
                        && !PatternUtils.isBlockPattern(positions, from, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                    IrregularMovements.cancel(p, from, prevention, "equilibrium-spacious",
                            jumpEffectASYNC || velocityScenarioASYNC ? 2 : 1);
                } else if (!knownBoxes
                        && !GroundUtils.heightExists(oldBox - oldVertical)) {
                    IrregularMovements.cancel(p, from, prevention, "*equilibrium-abstract", 2);
                } else {
                    IrregularMovements.cancel(p, from, prevention, "*equilibrium-predictable", 3);
                }
            }
        });
    }
}
