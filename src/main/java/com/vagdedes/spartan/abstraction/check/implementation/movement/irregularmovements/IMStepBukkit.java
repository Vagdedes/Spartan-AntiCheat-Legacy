package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.EntityUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PotionEffectUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;

import java.util.LinkedHashMap;
import java.util.Map;

public class IMStepBukkit extends CheckDetection {

    private static final double bedrockPlayerStepExtra = 1.0;
    private static final Map<Integer, Boolean> stepCombination = new LinkedHashMap<>();

    static {
        for (double before : GroundUtils.getHeights()) {
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
        return (Double.hashCode(before) * PluginBase.hashCodeMultiplier)
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

    IMStepBukkit(CheckRunner executor) {
        super(executor, null, null, null, true);
        this.step_high = new ImplementedDetection(this, "step_high", true);
        this.step_combination = new ImplementedDetection(this, "step_combination", true);
        this.step_entity = new ImplementedDetection(this, "step_entity", true);
        this.step_abstract_up = new ImplementedDetection(this, "step_abstract_up", true);
        this.step_high_first_fall = new ImplementedDetection(this, "step_high_first_fall", true);
        this.step_landing = new ImplementedDetection(this, "step_landing", true);
        this.step_abstract_down = new ImplementedDetection(this, "step_abstract_down", true);
    }

    void run(boolean knownBox, boolean knownOldBox,
             double box, double oldBox,
             double vertical, boolean teleported) {
        if (this.protocol.bukkitExtra.movement.wasGliding()) {
            return;
        }
        Entity vehicle = this.protocol.bukkitExtra.getVehicle();

        if (knownBox && knownOldBox) {
            int hash = hash(oldBox, box);

            if (vehicle == null) {
                if (stepCombination.get(hash)) {
                    this.step_high.call(() -> {
                        double limit = (this.protocol.isBedrockPlayer() ? bedrockPlayerStepExtra : 0.0)
                                + (this.protocol.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "exit")
                                || this.protocol.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY) ? 1.0 : 0.0)
                                + GroundUtils.maxPlayerStep;

                        if (vertical > limit) {
                            this.step_high.cancel(
                                    "step(high)"
                                            + ", box: " + box
                                            + ", old-box: " + oldBox
                                            + ", vertical: " + vertical
                                            + ", limit: " + limit,
                                    this.protocol.getFromLocation(),
                                    0,
                                    true
                            );
                        }
                    });
                } else {
                    this.step_combination.call(() -> {
                        double extra = this.protocol.isBedrockPlayer() ? bedrockPlayerStepExtra : 0.0,
                                downLimit = extra + this.getAirAcceleration(),
                                upLimit = extra + GroundUtils.maxPlayerStep;

                        if (!this.protocol.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "exit") && vertical < -downLimit
                                || !this.protocol.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "enter") && vertical > upLimit) {
                            this.step_combination.cancel(
                                    "step(combination)"
                                            + ", box: " + box
                                            + ", old-box: " + oldBox
                                            + ", vertical: " + vertical
                                            + ", down-limit: " + downLimit
                                            + ", up-limit: " + upLimit,
                                    this.protocol.getFromLocation(),
                                    0,
                                    true
                            );
                        }
                    });
                }
            } else {
                this.step_entity.call(() -> {
                    double limit = vertical % 1.0 != 0.0
                            && GroundUtils.blockHeightExists(box)
                            && GroundUtils.blockHeightExists(oldBox) ? 4.0 : 1.0;

                    if (vertical > limit) {
                        this.step_entity.cancel(
                                "step(entity)"
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", vertical: " + vertical
                                        + ", limit: " + limit,
                                this.protocol.getFromLocation(),
                                0,
                                true
                        );
                    }
                });
            }
        } else {
            if (vertical > 0.0) {
                this.step_abstract_up.call(() -> {
                    if (!this.protocol.bukkitExtra.movement.isJumping(vertical)
                            && !BlockUtils.isInteractiveAndPassable(this.protocol.bukkitExtra, new SpartanLocation(this.protocol.getLocation()).getBlock().getType())) {
                        double limit = (this.protocol.isBedrockPlayer() ? bedrockPlayerStepExtra : 0.0)
                                + (this.protocol.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY) ? 1.0 : 0.0)
                                + PlayerUtils.getJumpMotion(
                                PlayerUtils.getPotionLevel(this.protocol.bukkitExtra, PotionEffectUtils.JUMP) + 1,
                                0
                        );

                        if (vehicle != null) {
                            double vehicleLimit = 0.0;

                            if (EntityUtils.abstractHorseClass) {
                                if (vehicle instanceof AbstractHorse) {
                                    vehicleLimit = ((AbstractHorse) vehicle).getJumpStrength()
                                            + GroundUtils.maxHeightLengthRatio;
                                }
                            } else if (vehicle instanceof Horse) {
                                vehicleLimit = ((Horse) vehicle).getJumpStrength()
                                        + GroundUtils.maxHeightLengthRatio;
                            }

                            if (vehicleLimit > 0.0) {
                                if (this.protocol.isBedrockPlayer()) {
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
                                    && (!this.protocol.trackers.has(PlayerTrackers.TrackerType.BOUNCING_BLOCKS, "slime") || oldBox != 0.0)
                                    && (!this.protocol.trackers.has(PlayerTrackers.TrackerType.BOUNCING_BLOCKS, "bed") || oldBox != 0.5625)) {
                                this.step_abstract_up.cancel(
                                        "step(abstract-up)"
                                                + ", box: " + box
                                                + ", old-box: " + oldBox
                                                + ", vertical: " + vertical
                                                + ", limit: " + limit,
                                        this.protocol.getFromLocation(),
                                        0,
                                        true
                                );
                            }
                        }
                    }
                });
            } else {
                if (vehicle == null
                        && !this.protocol.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "exit")) {
                    if (knownBox) {
                        this.step_landing.call(() -> {
                            if (!teleported) {
                                double verticalPrediction = oldBox - box,
                                        motionRemainder = 1.0 - AlgebraUtils.removeBase(vertical);

                                if (verticalPrediction < 0.0) {
                                    verticalPrediction += 1.0;
                                }
                                if (verticalPrediction != motionRemainder) {
                                    this.step_landing.cancel(
                                            "step(landing)"
                                                    + ", box: " + box
                                                    + ", old-box: " + oldBox
                                                    + ", vertical: " + vertical
                                                    + ", vertical-prediction: " + verticalPrediction
                                                    + ", motion-remainder: " + motionRemainder,
                                            this.protocol.getFromLocation(),
                                            0,
                                            true
                                    );
                                }
                            }
                        });
                    } else {
                        this.step_abstract_down.call(() -> {
                            if (!this.protocol.isOnGround()
                                    && !BlockUtils.isInteractiveAndPassable(this.protocol.bukkitExtra, new SpartanLocation(this.protocol.getLocation()).getBlock().getType())) {
                                double limit = (this.protocol.isBedrockPlayer() ? bedrockPlayerStepExtra : 0.0)
                                        + this.getAirAcceleration();

                                if (vertical <= -limit) {
                                    this.step_abstract_down.cancel(
                                            "step(abstract-down)"
                                                    + ", box: " + box
                                                    + ", old-box: " + oldBox
                                                    + ", vertical: " + vertical
                                                    + ", limit: " + limit,
                                            this.protocol.getFromLocation(),
                                            0,
                                            true
                                    );
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private double getAirAcceleration() {
        return new SpartanLocation(this.protocol.getLocationOrVehicle()).isChunkLoaded()
                ? PlayerUtils.airAcceleration
                : PlayerUtils.airAccelerationUnloaded;
    }

}
