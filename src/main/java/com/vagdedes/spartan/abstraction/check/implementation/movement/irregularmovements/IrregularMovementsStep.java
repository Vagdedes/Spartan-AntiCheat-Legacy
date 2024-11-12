package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetectionExecutor;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PotionEffectUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IrregularMovementsStep extends ProbabilityDetectionExecutor {

    private static final double bedrockPlayerStepExtra = 1.0;
    private static final boolean abstractHorseClass = ReflectionUtils.classExists(
            "org.bukkit.entity.AbstractHorse"
    );
    private static final Map<Integer, Boolean> stepCombination = new LinkedHashMap<>();
    private static final Set<Double> firstFallingBox = new HashSet<>();

    static {
        for (double before : GroundUtils.getHeights()) {
            firstFallingBox.add(AlgebraUtils.cut(
                    AlgebraUtils.removeBase(before - (PlayerUtils.airAcceleration * PlayerUtils.airDrag)),
                    GroundUtils.maxHeightLength
            ));
            firstFallingBox.add(AlgebraUtils.cut(
                    AlgebraUtils.removeBase(before - (PlayerUtils.airAccelerationUnloaded * PlayerUtils.airDrag)),
                    GroundUtils.maxHeightLength
            ));
            firstFallingBox.add(AlgebraUtils.cut(
                    AlgebraUtils.removeBase(before - (PlayerUtils.slowFallAcceleration * PlayerUtils.airDrag)),
                    GroundUtils.maxHeightLength
            ));
            firstFallingBox.add(AlgebraUtils.cut(
                    AlgebraUtils.removeBase(before - (PlayerUtils.liquidAcceleration * PlayerUtils.waterDrag)),
                    GroundUtils.maxHeightLength
            ));
            firstFallingBox.add(AlgebraUtils.cut(
                    AlgebraUtils.removeBase(before - (PlayerUtils.liquidAcceleration * PlayerUtils.lavaDrag)),
                    GroundUtils.maxHeightLength
            ));

            for (double after : GroundUtils.getHeights()) {
                int hash = hash(before, after);

                if (after == before) {
                    stepCombination.put(hash, true);
                } else if (after > before) {
                    stepCombination.put(hash, after - before <= GroundUtils.maxPlayerStep);
                } else {
                    stepCombination.put(hash, before - after <= PlayerUtils.airAcceleration);
                }
            }
        }
    }

    private static int hash(double before, double after) {
        return (Double.hashCode(before) * SpartanBukkit.hashCodeMultiplier)
                + Double.hashCode(after);
    }

    // Separator

    private final CheckDetection
            step_high,
            step_combination,
            step_entity,
            step_abstract_up,
            step_high_first_fall,
            step_landing,
            step_abstract_down;

    IrregularMovementsStep(CheckRunner executor) {
        super(executor, null, true);
        this.step_high = new ImplementedProbabilityDetection(this, "step_high", true);
        this.step_combination = new ImplementedProbabilityDetection(this, "step_combination", true);
        this.step_entity = new ImplementedProbabilityDetection(this, "step_entity", true);
        this.step_abstract_up = new ImplementedProbabilityDetection(this, "step_abstract_up", true);
        this.step_high_first_fall = new ImplementedProbabilityDetection(this, "step_high_first_fall", true);
        this.step_landing = new ImplementedProbabilityDetection(this, "step_landing", true);
        this.step_abstract_down = new ImplementedProbabilityDetection(this, "step_abstract_down", true);
    }

    void run(boolean knownBox, boolean knownOldBox,
             double box, double oldBox,
             double vertical, boolean teleported) {
        Entity vehicle = this.protocol().spartan.getVehicle();

        if (knownBox && knownOldBox) {
            int hash = hash(oldBox, box);

            if (vehicle == null) {
                if (stepCombination.get(hash)) {
                    double limit = (this.protocol().spartan.isBedrockPlayer() ? bedrockPlayerStepExtra : 0.0)
                            + (this.protocol().spartan.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "exit")
                            || this.protocol().spartan.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY) ? 1.0 : 0.0)
                            + GroundUtils.maxPlayerStep;

                    if (vertical > limit) {
                        this.step_high.cancel(
                                "type: step(high)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical
                                        + ", limit: " + limit,
                                vertical,
                                this.protocol().spartan.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                } else {
                    double extra = this.protocol().spartan.isBedrockPlayer() ? bedrockPlayerStepExtra : 0.0,
                            downLimit = extra + this.protocol().spartan.movement.getAirAcceleration(),
                            upLimit = extra + GroundUtils.maxPlayerStep;

                    if (!this.protocol().spartan.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "exit") && vertical < -downLimit
                            || !this.protocol().spartan.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "enter") && vertical > upLimit) {
                        this.step_combination.cancel(
                                "type: step(combination)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical
                                        + ", down-limit: " + downLimit
                                        + ", up-limit: " + upLimit,
                                Math.abs(vertical),
                                this.protocol().spartan.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                }
            } else {
                double limit = vertical % 1.0 != 0.0
                        && GroundUtils.blockHeightExists(box)
                        && GroundUtils.blockHeightExists(oldBox) ? 4.0 : 1.0;

                if (vertical > limit) {
                    this.step_entity.cancel(
                            "type: step(entity)"
                                    + ", box: " + box
                                    + ", old-box: " + oldBox
                                    + ", vertical: " + vertical
                                    + ", limit: " + limit,
                            vertical / limit,
                            this.protocol().spartan.movement.getDetectionLocation(),
                            0,
                            true
                    );
                }
            }
        } else {
            if (vertical > 0.0) {
                if (!this.protocol().spartan.movement.isJumping(vertical)
                        && !BlockUtils.isInteractiveAndPassable(this.protocol().spartan, this.protocol().spartan.movement.getLocation().getBlock().getType())) {
                    double limit = (this.protocol().spartan.isBedrockPlayer() ? bedrockPlayerStepExtra : 0.0)
                            + (this.protocol().spartan.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY) ? 1.0 : 0.0)
                            + PlayerUtils.getJumpMotion(
                            PlayerUtils.getPotionLevel(this.protocol().spartan, PotionEffectUtils.JUMP) + 1,
                            0
                    );

                    if (vehicle != null) {
                        double vehicleLimit = 0.0;

                        if (abstractHorseClass) {
                            if (vehicle instanceof AbstractHorse) {
                                vehicleLimit = ((AbstractHorse) vehicle).getJumpStrength()
                                        + GroundUtils.maxHeightLengthRatio;
                            }
                        } else if (vehicle instanceof Horse) {
                            vehicleLimit = ((Horse) vehicle).getJumpStrength()
                                    + GroundUtils.maxHeightLengthRatio;
                        }

                        if (vehicleLimit > 0.0) {
                            if (this.protocol().spartan.isBedrockPlayer()) {
                                limit = Math.max(limit, vehicleLimit);
                            } else {
                                limit = vehicleLimit;
                            }
                        }
                    }

                    if (!teleported && vertical > limit) {
                        double predictedHeight = box - oldBox;

                        if (predictedHeight < 0.0) {
                            predictedHeight += 1.0;
                        }

                        if ((predictedHeight != vertical
                                || vertical > GroundUtils.maxPlayerStep)
                                && (!this.protocol().spartan.trackers.has(PlayerTrackers.TrackerType.BOUNCING_BLOCKS, "slime") || oldBox != 0.0)
                                && (!this.protocol().spartan.trackers.has(PlayerTrackers.TrackerType.BOUNCING_BLOCKS, "bed") || oldBox != 0.5625)) {
                            this.step_abstract_up.cancel(
                                    "type: step(abstract-up)"
                                            + ", box: " + box
                                            + ", old-box: " + oldBox
                                            + ", vertical: " + vertical
                                            + ", limit: " + limit,
                                    vertical / limit,
                                    this.protocol().spartan.movement.getDetectionLocation(),
                                    0,
                                    true
                            );
                        }
                    }
                }
            } else {
                if (firstFallingBox.contains(AlgebraUtils.cut(box, GroundUtils.maxHeightLength))) {
                    if (vertical < -this.protocol().spartan.movement.getAirAcceleration()) {
                        this.step_high_first_fall.cancel(
                                "type: step(high-first-fall)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical,
                                0.0d - vertical,
                                this.protocol().spartan.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                } else if (vehicle == null
                        && !this.protocol().spartan.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "exit")) {
                    if (knownBox) {
                        if (!teleported) {
                            double verticalPrediction = oldBox - box,
                                    motionRemainder = 1.0 - AlgebraUtils.removeBase(vertical);

                            if (verticalPrediction < 0.0) {
                                verticalPrediction += 1.0;
                            }
                            if (verticalPrediction != motionRemainder) {
                                this.step_landing.cancel(
                                        "type: step(landing)"
                                                + ", box: " + box
                                                + ", old-box: " + oldBox
                                                + ", vertical: " + vertical
                                                + ", vertical-prediction: " + verticalPrediction
                                                + ", motion-remainder: " + motionRemainder,
                                        1.0,
                                        this.protocol().spartan.movement.getDetectionLocation(),
                                        0,
                                        true
                                );
                            }
                        }
                    } else if (!this.protocol().spartan.isOnGround(this.protocol().spartan.movement.getLocation(), true)
                            && !BlockUtils.isInteractiveAndPassable(this.protocol().spartan, this.protocol().spartan.movement.getLocation().getBlock().getType())) {
                        double limit = (this.protocol().spartan.isBedrockPlayer() ? bedrockPlayerStepExtra : 0.0)
                                + this.protocol().spartan.movement.getAirAcceleration();

                        if (vertical <= -limit) {
                            this.step_abstract_down.cancel(
                                    "type: step(abstract-down)"
                                            + ", box: " + box
                                            + ", old-box: " + oldBox
                                            + ", vertical: " + vertical
                                            + ", limit: " + limit,
                                    0.0d - vertical,
                                    this.protocol().spartan.movement.getDetectionLocation(),
                                    0,
                                    true
                            );
                        }
                    }
                }
            }
        }
    }

}
