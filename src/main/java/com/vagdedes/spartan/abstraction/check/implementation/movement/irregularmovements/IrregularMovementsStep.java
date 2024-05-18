package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.ReflectionUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.GroundUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IrregularMovementsStep extends DetectionExecutor {

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

    IrregularMovementsStep(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(boolean knownBox, boolean knownOldBox, double box, double oldBox, double vertical) {
        if (hackType.getCheck().getBooleanOption("check_step", true)) {
            Entity vehicle = player.getVehicle();
            boolean slime = player.trackers.has(Trackers.TrackerType.BOUNCING_BLOCKS, "slime"),
                    bed = player.trackers.has(Trackers.TrackerType.BOUNCING_BLOCKS, "bed"),
                    vehicleEnter = player.trackers.has(Trackers.TrackerType.VEHICLE, "enter");

            runAsync(() -> {
                if (knownBox && knownOldBox) {
                    int hash = hash(oldBox, box);

                    if (vehicle == null) {
                        if (stepCombination.get(hash)) {
                            double limit = (player.bedrockPlayer ? 1.0 : 0.0) + GroundUtils.maxPlayerStep;

                            if (vertical > limit) {
                                cancel(
                                        "type: step(high)"
                                                + ", box: " + box
                                                + ", old-box: " + oldBox
                                                + ", vertical: " + vertical
                                                + ", limit: " + limit,
                                        vertical,
                                        player.movement.getDetectionLocation(),
                                        0,
                                        true
                                );
                            }
                        } else if (vertical < -PlayerUtils.airAcceleration
                                || vertical > GroundUtils.maxPlayerStep) {
                            cancel(
                                    "type: step(combination)"
                                            + ", box: " + box
                                            + ", old-box: " + oldBox
                                            + ", vertical: " + vertical,
                                    Math.abs(vertical),
                                    player.movement.getDetectionLocation(),
                                    0,
                                    true
                            );
                        }
                    } else if (vertical > 1.0) {
                        cancel(
                                "type: step(entity)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical,
                                vertical - 1.0,
                                player.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                } else {
                    if (vertical > 0.0) {
                        if (!player.movement.isJumping(vertical)) {
                            double limit = PlayerUtils.getJumpMotion(
                                    PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1,
                                    0
                            );

                            if (vehicle != null) {
                                if (abstractHorseClass) {
                                    if (vehicle instanceof AbstractHorse) {
                                        limit = Math.max(
                                                limit,
                                                ((AbstractHorse) vehicle).getJumpStrength()
                                                        + GroundUtils.maxHeightLengthRatio
                                        );
                                    }
                                } else if (vehicle instanceof Horse) {
                                    limit = Math.max(
                                            limit,
                                            ((Horse) vehicle).getJumpStrength()
                                                    + GroundUtils.maxHeightLengthRatio
                                    );
                                }
                            }
                            if (vertical > limit) {
                                double predictedHeight = box - oldBox;

                                if (predictedHeight < 0.0) {
                                    predictedHeight += 1.0;
                                }

                                if ((predictedHeight != vertical
                                        || vertical > GroundUtils.maxPlayerStep)
                                        && (!slime || oldBox != 0.0)
                                        && (!bed || oldBox != 0.5625)) {
                                    cancel(
                                            "type: step(abstract-up)"
                                                    + ", box: " + box
                                                    + ", old-box: " + oldBox
                                                    + ", vertical: " + vertical,
                                            vertical,
                                            player.movement.getDetectionLocation(),
                                            0,
                                            true
                                    );
                                }
                            }
                        }
                    } else {
                        if (firstFallingBox.contains(AlgebraUtils.cut(box, GroundUtils.maxHeightLength))) {
                            if (vertical < -PlayerUtils.airAcceleration) {
                                cancel(
                                        "type: step(high-first-fall)"
                                                + ", box: " + box
                                                + ", old-box: " + oldBox
                                                + ", vertical: " + vertical,
                                        0.0d - vertical,
                                        player.movement.getDetectionLocation(),
                                        0,
                                        true
                                );
                            }
                        } else if (knownBox) {
                            double verticalPrediction = oldBox - box,
                                    motionRemainder = 1.0 - AlgebraUtils.removeBase(vertical);

                            if (verticalPrediction < 0.0) {
                                verticalPrediction += 1.0;
                            }
                            if (!vehicleEnter && verticalPrediction != motionRemainder) {
                                cancel(
                                        "type: step(landing)"
                                                + ", box: " + box
                                                + ", old-box: " + oldBox
                                                + ", vertical: " + vertical
                                                + ", vertical-prediction: " + verticalPrediction
                                                + ", motion-remainder: " + motionRemainder,
                                        1.0,
                                        player.movement.getDetectionLocation(),
                                        0,
                                        true
                                );
                            }
                        } else if (vertical < -PlayerUtils.airAcceleration) {
                            cancel(
                                    "type: step(abstract-down)"
                                            + ", box: " + box
                                            + ", old-box: " + oldBox
                                            + ", vertical: " + vertical,
                                    0.0d - vertical,
                                    player.movement.getDetectionLocation(),
                                    0,
                                    true
                            );
                        }
                    }
                }
            });
        }
    }

}
