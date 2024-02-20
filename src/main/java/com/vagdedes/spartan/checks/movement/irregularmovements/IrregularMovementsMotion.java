package com.vagdedes.spartan.checks.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.objects.data.Cooldowns;
import com.vagdedes.spartan.objects.data.Decimals;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.utils.gameplay.*;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Material;

public class IrregularMovementsMotion extends DetectionExecutor {

    private final IrregularMovements irregularMovements;

    IrregularMovementsMotion(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        irregularMovements = (IrregularMovements) executor;
    }

    private static final double
            criticalDecelerationLimit = -0.1,
            criticalUpwardsLimit = Math.sqrt(MoveUtils.chunk), // 4.0
            postJumpPreFallPrecision = MoveUtils.jumping[2] * 7, // 0.0007
            criticalStepLimit = GroundUtils.maxPlayerStep * 2.0, // 1.325
            vehicleStep = 2.0;
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

    private static final double[][]
            honey = new double[][]{
            {1, 1, 1},
            {1, 0, 1},
    }, jumpEnd = new double[][]{
            {1, 1, 1},
            {1, 0, 1},
            {1, -11, 1},
    };

    private void clearGeneralCache(IrregularMovements executor, Cooldowns cooldowns) {
        executor.forgetBoolean(cooldowns, motionJumpKey);
        executor.forgetBoolean(cooldowns, motionVelocityKey);
    }

    private void clearDetectionCache(IrregularMovements executor, Decimals decimals) {
        executor.clear(executor.player, upwardsDecelerationKey);
        // no Decimals usage

        executor.clear(executor.player, upwardsAccelerationKey);
        // no Decimals usage

        executor.clear(executor.player, upwardsProgressiveKey);
        // no Decimals usage

        executor.clear(executor.player, downwardsUnexpectedKey);
        // no Decimals usage

        executor.clear(executor.player, downwardsDifferenceKey);
        decimals.remove(downwardsDifferenceKey);

        executor.clear(executor.player, downwardsPredictionKey);
        decimals.remove(downwardsPredictionKey);
    }

    void run(Cooldowns cooldowns,
             double verticalDifference,
             double horizontalDifference,
             boolean knownBoxes, boolean knownBoxesNew, boolean knownBoxesOld,
             boolean offGround, boolean onGroundCustom,
             boolean oldFalling, boolean levitationEffect,
             boolean constructDetection, boolean groundInteractionScenario, boolean velocityScenario, boolean changedScenario, boolean climbScenario, boolean slowFallingScenario,
             boolean checkUp, boolean checkDown, boolean checkJump, boolean checkFall, boolean checkHorizontal,
             String prevention) {
        Double vertical = player.getNmsVerticalDistance(),
                oldVertical = player.getPreviousNmsVerticalDistance(),
                box = player.getNmsBox(),
                oldBox = player.getOld_NmsBox();

        if (vertical == null
                || oldVertical == null
                || box == null
                || oldBox == null) {
            return;
        }
        SpartanLocation to = player.getEventToLocation(),
                from = player.getEventFromLocation();
        int airTicks = player.getTicksOnAir();
        boolean hasVehicle = player.getHandlers().has(Handlers.HandlerType.Vehicle);

        Check check = hackType.getCheck();
        double stepLimit;

        if (hasVehicle) {
            double def = check.getDecimalOption("step_limit_in_blocks", GroundUtils.maxPlayerStep);
            stepLimit = def <= 0.0 ? def : Math.max(def, vehicleStep);
        } else {
            stepLimit = check.getDecimalOption("step_limit_in_blocks", GroundUtils.maxPlayerStep);
        }
        boolean jumpEffect = PlayerData.hasJumpEffect(player),
                checkHoppingASYNC = (checkUp || checkJump) && check.getBooleanOption("check_hopping", true),
                checkUpASYNC = checkUp && check.getBooleanOption("check_upwards", true),
                checkDownASYNC = checkDown && check.getBooleanOption("check_downwards", true),
                checkJumpASYNC = checkJump && !hasVehicle && check.getBooleanOption("check_jumping", true),
                checkFallASYNC = checkFall && check.getBooleanOption("check_falling", true),
                checkHorizontalASYNC = checkHorizontal && check.getBooleanOption("check_horizontal", true),
                onGround = onGroundCustom || airTicks == 0;

        // Separator
        runAsync(() -> {
            Decimals decimals = player.getDecimals(hackType);
            double criticalDecelerationLimit;
            boolean jumpEffectASYNC, velocityScenarioASYNC;

            if (onGround) { // Ground
                clearGeneralCache(irregularMovements, cooldowns);
                clearDetectionCache(irregularMovements, decimals);
                jumpEffectASYNC = jumpEffect;
                velocityScenarioASYNC = velocityScenario;
                criticalDecelerationLimit = jumpEffectASYNC ?
                        (IrregularMovementsMotion.criticalDecelerationLimit * 3) :
                        IrregularMovementsMotion.criticalDecelerationLimit;
            } else { // Motion
                jumpEffectASYNC = irregularMovements.cacheBoolean(cooldowns, jumpEffect, motionJumpKey, extendedDefaultBufferTime);
                velocityScenarioASYNC = irregularMovements.cacheBoolean(cooldowns, velocityScenario, motionVelocityKey, extendedDefaultBufferTime);
                criticalDecelerationLimit = jumpEffectASYNC ?
                        (IrregularMovementsMotion.criticalDecelerationLimit * 3) :
                        IrregularMovementsMotion.criticalDecelerationLimit;

                if (vertical > 0.0) {
                    if (checkUpASYNC
                            && !knownBoxesNew
                            && !player.isJumping(vertical)
                            && Math.abs(vertical - MoveUtils.postJumpPreFall) >= postJumpPreFallPrecision) {
                        boolean abstractScenario = groundInteractionScenario || velocityScenarioASYNC;

                        if (stepLimit > 0.0) {
                            double upwardsLimit = Math.max(GroundUtils.maxPlayerStep, stepLimit);

                            if (jumpEffect) {
                                upwardsLimit += player.getExtraMovementFromJumpEffect();
                            }
                            if (abstractScenario
                                    || PatternUtils.isBlockPattern(honey, to, true, BlockUtils.honey_block)) {
                                upwardsLimit = Math.max(upwardsLimit, criticalUpwardsLimit);
                            }
                            if (vertical > upwardsLimit) {
                                irregularMovements.cancel(prevention,
                                        "upwards-instant", "limit: " + upwardsLimit,
                                        1, 0);
                            }
                        }
                        if (verticalDifference <= criticalDecelerationLimit) {
                            irregularMovements.cancel(prevention,
                                    upwardsDecelerationKey, null,
                                    5, extendedDefaultBufferTime);
                        }
                        if (jumpEffectASYNC || abstractScenario) {
                            if (verticalDifference >= vertical) {
                                irregularMovements.cancel(prevention,
                                        upwardsAccelerationKey, null,
                                        hasVehicle ? 15 : 5, extendedDefaultBufferTime);
                            }
                        } else {
                            irregularMovements.cancel(prevention,
                                    upwardsProgressiveKey, null,
                                    hasVehicle ? 15 : 5, extendedDefaultBufferTime);
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
                            int fallingToAirTicksDifference = airTicks - player.getFallingTicks(); // Do not consider when air-ticks are less than falling-ticks

                            if (fallingToAirTicksDifference >= criticalTickDifference) {
                                if (decimals.get(downwardsDifferenceKey) != fallingToAirTicksDifference) {
                                    irregularMovements.cancel(prevention,
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
                                ), limit = player.getHandlers().has(Handlers.HandlerType.Vehicle) ? 12 : 3;

                                if (inaccuracy >= limit) {
                                    irregularMovements.cancel(
                                            prevention, downwardsPredictionKey, "inaccuracy: " + inaccuracy + ", limit: " + limit,
                                            5, extendedDefaultBufferTime);
                                }
                            }
                            decimals.set(
                                    downwardsPredictionKey,
                                    MoveUtils.getFallingTick((vertical * MoveUtils.dragComplement) - MoveUtils.gravityAcceleration)
                            );
                        } else {
                            decimals.remove(downwardsPredictionKey);
                            irregularMovements.cancel(
                                    prevention, downwardsUnexpectedKey, null,
                                    5, extendedDefaultBufferTime);
                        }
                    }
                } else {
                    clearDetectionCache(irregularMovements, decimals);
                }
            }

            // Separator

            if (checkHoppingASYNC
                    && !constructDetection
                    && !velocityScenarioASYNC
                    && !jumpEffectASYNC
                    && player.isJumping(vertical)) {
                if (player.isSprinting()) {
                    if (Math.abs(vertical - MoveUtils.jumping[1]) < MoveUtils.getJumpingPrecision(player)) {
                        if (horizontalDifference < 0.0) {
                            irregularMovements.cancel(
                                    prevention, "hopping-sprinting-deceleration", null,
                                    criticalHoppingBuffer, decreasedDefaultBufferTime);
                        }
                    } else if (horizontalDifference >= 0.03) { // Normally, is anything greater than 0.0 but this is more stable
                        irregularMovements.cancel(
                                prevention, "hopping-sprinting-acceleration", null,
                                criticalHoppingBuffer, decreasedDefaultBufferTime);
                    }
                } else {
                    if (Math.abs(vertical - MoveUtils.jumping[1]) < MoveUtils.getJumpingPrecision(player)) {
                        if (horizontalDifference < 0.0) {
                            irregularMovements.cancel(
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
                            irregularMovements.cancel(prevention, "step-up-predictable",
                                    "box-difference: " + boxDifference + ", limit: " + stepLimitModified,
                                    !velocityScenarioASYNC && boxDifference >= criticalStepLimit ? 1 : 2,
                                    velocityScenarioASYNC ? -IrregularMovements.defaultBufferTime : IrregularMovements.defaultBufferTime);
                        }
                    } else if (vertical < 0.0) {
                        double limit = hasVehicle ? vehicleStep : MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision;

                        if (boxDifference >= limit) { // Anything below this threshold is likely instant, where otherwise you fall for a bit
                            irregularMovements.cancel(prevention, "step-down-predictable",
                                    "box-difference: " + boxDifference + ", limit: " + limit,
                                    1, 0);
                        }
                    }
                } else if (knownBoxesNew) {
                    if (vertical > stepLimitModified) {
                        irregularMovements.cancel(prevention, "step-up-known", "limit: " + stepLimitModified,
                                !velocityScenarioASYNC && vertical >= criticalStepLimit ? 1 : 2,
                                velocityScenarioASYNC ? -IrregularMovements.defaultBufferTime : IrregularMovements.defaultBufferTime);
                    } else if (!oldFalling
                            && !velocityScenarioASYNC
                            && !jumpEffectASYNC
                            && vertical <= criticalDecelerationLimit) {
                        irregularMovements.cancel(prevention, "step-down-known", "limit: " + stepLimitModified,
                                vertical <= -criticalStepLimit ? 1 : 2, IrregularMovements.defaultBufferTime);
                    }
                }
            }

            if (vertical < oldVertical) { // Fall
                if (checkJumpASYNC) {
                    if (player.isJumping(vertical)) {
                        if (player.isJumping(oldVertical)) {
                            double difference = Math.abs(verticalDifference);

                            if (difference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Jumping motions must not skip each other
                                irregularMovements.cancel(prevention, "jump-up-skip", 1);
                            } else if (difference < MoveUtils.highPrecision) { // Jumping motions must not be the same
                                irregularMovements.cancel(prevention, "jump-up-equilibrium",
                                        groundInteractionScenario || velocityScenarioASYNC ? MoveUtils.jumpingMotions : 1);
                            }
                        }
                    } else if (!jumpEffect
                            && !onGround
                            && !climbScenario
                            && player.isJumping(oldVertical)
                            && !player.getHandlers().has(Handlers.HandlerType.ElytraWear)
                            && (vertical != 0.0 || !PlayerData.isUsable(player.getItemInHand().getType()))) {
                        double jumpingPrecision = MoveUtils.getJumpingPrecision(player);

                        if (Math.abs(oldVertical - MoveUtils.jumping[0]) >= jumpingPrecision // Jumping must end somewhere
                                && !PatternUtils.isBlockPattern(new double[][]{
                                {1, 3, 1},
                                {1, 2, 1},
                                {1, 2 - box, 1},
                                {1, 2 - oldBox, 1, oldBox != box ? 1 : 0},
                                {1, 1, 1},
                        }, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                            irregularMovements.cancel(prevention, "jump-end",
                                    groundInteractionScenario || velocityScenarioASYNC ? MoveUtils.jumpingMotions : 1);
                        }
                    }
                }
            } else if (vertical > oldVertical) { // Ascension
                if (player.isJumping(vertical)) {
                    if (checkJumpASYNC) {
                        if (player.isJumping(oldVertical)) {
                            double difference = Math.abs(verticalDifference);

                            if (difference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Jumping motions must not skip each other
                                irregularMovements.cancel(prevention, "jump-down-skip",
                                        groundInteractionScenario || velocityScenarioASYNC ? MoveUtils.jumpingMotions : 1);
                            } else if (difference < MoveUtils.highPrecision) { // Jumping motions must not be the same
                                irregularMovements.cancel(prevention, "jump-down-equilibrium",
                                        groundInteractionScenario || velocityScenarioASYNC ? MoveUtils.jumpingMotions : 1);
                            }
                        } else if (!onGroundCustom
                                && Math.abs(vertical - MoveUtils.jumping[1]) < MoveUtils.getJumpingPrecision(player)
                                && (!GroundUtils.heightExists(Math.abs(box - vertical)) || !offGround
                                && to.areBlocks(Material.AIR, 1, false)
                                && to.clone().add(0, -1, 0).areBlocks(Material.AIR, 1, true)
                                && to.clone().add(0, 1, 0).areBlocks(Material.AIR, 1, true))) { // Jumping must start from somewhere
                            irregularMovements.cancel(prevention, "jump-start", 1);
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

                if (velocityScenarioASYNC) {
                    irregularMovements.cancel(prevention, "equilibrium-velocity", null,
                            MoveUtils.jumpingMotions, IrregularMovements.defaultBufferTime, horizontalDifference >= 0.01);
                } else if (levitationEffect) {
                    irregularMovements.cancel(prevention, "equilibrium-levitation", null,
                            MoveUtils.jumpingMotions, IrregularMovements.defaultBufferTime, horizontalDifference >= 0.01);
                } else {
                    Handlers handlers = player.getHandlers();

                    if (!handlers.has(Handlers.HandlerType.TowerBuilding)
                            && !handlers.has(Handlers.HandlerType.BridgeBuilding)) {
                        if (!PatternUtils.isBlockPattern(positions, to, true, BlockUtils.solid, BlockUtils.semi_solid)
                                && !PatternUtils.isBlockPattern(positions, from, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                            irregularMovements.cancel(prevention, "equilibrium-spacious", null,
                                    jumpEffectASYNC ? 2 : 1, IrregularMovements.defaultBufferTime, horizontalDifference >= 0.01);
                        } else if (!knownBoxes
                                && !GroundUtils.heightExists(oldBox - oldVertical)) {
                            irregularMovements.cancel(prevention, "*equilibrium-abstract", null,
                                    2, IrregularMovements.defaultBufferTime, horizontalDifference >= 0.01);
                        } else {
                            irregularMovements.cancel(prevention, "*equilibrium-predictable", null,
                                    3, IrregularMovements.defaultBufferTime, horizontalDifference >= 0.01);
                        }
                    }
                }
            }
        });
    }
}
