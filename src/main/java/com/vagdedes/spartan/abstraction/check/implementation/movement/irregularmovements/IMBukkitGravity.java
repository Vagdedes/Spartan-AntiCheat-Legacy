package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PotionEffectUtils;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class IMBukkitGravity extends ProbabilityDetection {

    private static final Material MACE = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)
            ? MaterialUtils.findMaterial("MACE")
            : null;

    // Separator

    private final CheckDetection
            gravity_climb_stable,
            gravity_prediction;
    private boolean ignore, sensitive;

    IMBukkitGravity(CheckRunner executor) {
        super(executor, null, true);
        this.gravity_climb_stable = new ImplementedProbabilityDetection(executor, "climb", true);
        this.gravity_prediction = new ImplementedProbabilityDetection(executor, "gravity_bukkit", true);
    }

    final void run(double vertical, double oldVertical, double box, double oldBox) {
        if (this.protocol.packetsEnabled()
                || isUsingMace()) {
            return;
        }
        int airTicks = this.protocol.spartan.movement.getTicksOnAir();
        SpartanLocation to = new SpartanLocation(this.protocol.getLocation()),
                from = new SpartanLocation(this.protocol.getFromLocation());
        int levitationLevel = PlayerUtils.levitation
                ? PlayerUtils.getPotionLevel(this.protocol.spartan, PotionEffectType.LEVITATION)
                : -1;
        List<Double> predictions = new ArrayList<>(5);
        boolean levitation = levitationLevel != -1,
                slowFall = PlayerUtils.slowFall && this.protocol.spartan.hasPotionEffect(PotionEffectType.SLOW_FALLING, 0),
                liquid = this.protocol.spartan.movement.isInLiquids(),
                climb = this.protocol.spartan.isFrozen() || canClimb(to) || canClimb(from),
                web = BlockPatternUtils.isBlockPattern(
                        new double[][]{
                                {GroundUtils.contactBoundingBox, 0.0, GroundUtils.contactBoundingBox},
                                {GroundUtils.contactBoundingBox, 1.0, GroundUtils.contactBoundingBox},
                                {GroundUtils.contactBoundingBox, Math.ceil(this.protocol.bukkit().getEyeHeight()), GroundUtils.contactBoundingBox}
                        },
                        to,
                        true,
                        BlockUtils.web
                ) || BlockPatternUtils.isBlockPattern(
                        new double[][]{
                                {GroundUtils.contactBoundingBox, 0.0, GroundUtils.contactBoundingBox},
                                {GroundUtils.contactBoundingBox, 1.0, GroundUtils.contactBoundingBox},
                                {GroundUtils.contactBoundingBox, Math.ceil(this.protocol.bukkit().getEyeHeight()), GroundUtils.contactBoundingBox}
                        },
                        from,
                        true,
                        BlockUtils.web
                ),
                honey = !web && (BlockPatternUtils.isBlockPattern(
                        new double[][]{
                                {GroundUtils.contactBoundingBox, 0.0, GroundUtils.contactBoundingBox},
                                {GroundUtils.contactBoundingBox, 1.0, GroundUtils.contactBoundingBox}
                        },
                        to,
                        true,
                        BlockUtils.honey_block
                ) || BlockPatternUtils.isBlockPattern(
                        new double[][]{
                                {GroundUtils.contactBoundingBox, 0.0, GroundUtils.contactBoundingBox},
                                {GroundUtils.contactBoundingBox, 1.0, GroundUtils.contactBoundingBox}
                        },
                        from,
                        true,
                        BlockUtils.honey_block
                ));

        if (levitation) {
            predictions.add(
                    (oldVertical + ((0.05 * (levitationLevel + 1) - oldVertical) * 0.2)) * PlayerUtils.airDrag
            );
        }
        predictions.add( // Always second
                PlayerUtils.calculateNextFallMotion(
                        oldVertical,
                        -this.protocol.spartan.movement.getAirAcceleration(),
                        PlayerUtils.airDrag
                )
        );

        if (web) {
            if (levitation) {
                predictions.add(predictions.get(0) - PlayerUtils.webBlockDownDefault);
            }
            predictions.add(-PlayerUtils.webBlockDownDefault);
        }
        if (liquid) {
            double calculation = PlayerUtils.calculateNextFallMotion(
                    oldVertical,
                    -PlayerUtils.liquidAcceleration,
                    this.protocol.spartan.movement.getLastLiquidMaterial() == Material.LAVA
                            ? PlayerUtils.lavaDrag
                            : PlayerUtils.waterDrag
            );
            if (levitation) {
                predictions.add(predictions.get(0) - calculation);
            }
            predictions.add(-calculation);
        }
        if (slowFall) {
            if (levitation) {
                predictions.add(predictions.get(0) - PlayerUtils.slowFallAcceleration);
            }
            predictions.add(-PlayerUtils.slowFallAcceleration);
        }
        if (climb) {
            if (vertical == 0.0 && oldVertical == 0.0) {
                if (!this.protocol.spartan.isOnGround(to.bukkit(), true)
                        && !this.protocol.isSneaking()
                        && this.protocol.spartan.getNearbyEntities(1.0, 1.0, 1.0).isEmpty()
                        && !BlockPatternUtils.isBlockPattern(
                        new double[]{
                                GroundUtils.boundingBox,
                                Math.ceil(this.protocol.bukkit().getEyeHeight()),
                                GroundUtils.boundingBox
                        },
                        to,
                        true,
                        BlockUtils.solid, BlockUtils.semi_solid
                )) {
                    this.gravity_climb_stable.cancel(
                            "type: gravity(climb-stable)"
                                    + ", vertical: " + vertical
                                    + ", old-vertical: " + oldVertical
                                    + ", box: " + box
                                    + ", old-box: " + oldBox
                                    + ", air-ticks: " + airTicks,
                            this.protocol.getFromLocation(),
                            0,
                            true
                    );
                }
                return;
            } else if (levitation) {
                predictions.add(PlayerUtils.climbingUpDefault);
                predictions.add(predictions.get(0) + PlayerUtils.climbingUpDefault);
            } else {
                predictions.set(0, PlayerUtils.climbingUpDefault);
                predictions.add(-PlayerUtils.climbingDownDefault);
            }
        }
        if (honey) {
            if (levitation) {
                predictions.add(predictions.get(0) - PlayerUtils.honeyBlockDownDefault);
            }
            predictions.add(-PlayerUtils.honeyBlockDownDefault);
        }
        boolean sensitive = predictions.size() > 1;
        this.ignore |= sensitive && !this.sensitive;
        this.sensitive = sensitive;
        String violation = null;

        for (double prediction : predictions) {
            if (Math.abs(prediction) > PlayerUtils.optimizationY) {
                double difference = Math.abs(vertical - prediction), product;

                if (this.protocol.spartan.isBedrockPlayer()) {
                    product = PlayerUtils.calculateTerminalVelocity(
                            PlayerUtils.airDrag,
                            this.protocol.spartan.movement.getAirAcceleration()
                    ) * 2;
                } else {
                    product = !sensitive
                            && !this.protocol.spartan.movement.isJumping(vertical) && !this.protocol.spartan.movement.isJumping(oldVertical)
                            && !this.protocol.spartan.movement.isFalling(vertical, -this.protocol.spartan.movement.getAirAcceleration(), PlayerUtils.airDrag, GroundUtils.maxHeightLengthRatio)
                            && !this.protocol.spartan.movement.isFalling(oldVertical, -this.protocol.spartan.movement.getAirAcceleration(), PlayerUtils.airDrag, GroundUtils.maxHeightLengthRatio)
                            ? GroundUtils.maxPlayerStep
                            : PlayerUtils.getJumpTicks(PlayerUtils.getPotionLevel(this.protocol.spartan, PotionEffectUtils.JUMP) + 1);
                }
                double limit = Math.max(
                        this.sensitive ? product : product / (double) airTicks,
                        this.protocol.spartan.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY)
                                || this.protocol.spartan.trackers.has(PlayerTrackers.TrackerFamily.MOTION) ? (GroundUtils.maxBoundingBox * 2.0) :
                                liquid ? GroundUtils.maxPlayerStep :
                                        this.protocol.spartan.isBedrockPlayer() || sensitive ? PlayerUtils.climbingDownDefault :
                                                GroundUtils.maxHeightLengthRatio
                );

                if (difference >= limit) {
                    violation = "type: gravity(prediction)"
                            + ", product: " + product
                            + ", limit: " + limit
                            + ", difference: " + difference
                            + ", prediction: " + prediction
                            + ", vertical: " + vertical
                            + ", old-vertical: " + oldVertical
                            + ", box: " + box
                            + ", old-box: " + oldBox
                            + ", air-ticks: " + airTicks
                            + ", sensitive: " + this.sensitive;
                } else {
                    return;
                }
            }
        }

        if (violation != null) {
            if (ignore) {
                ignore = false;
            } else {
                this.gravity_prediction.cancel(
                        violation,
                        this.protocol.getFromLocation(),
                        0,
                        true
                );
            }
        }
    }

    private static boolean canClimb(SpartanLocation location) {
        Material material = location.getBlock().getType();
        return BlockUtils.canClimb(material, false)
                || BlockUtils.areTrapdoors(material)
                && (BlockUtils.canClimb(location.clone().add(0, 1, 0).getBlock().getType(), false)
                || BlockUtils.canClimb(location.clone().add(0, -1, 0).getBlock().getType(), false));
    }

    private boolean isUsingMace() {
        if (MACE != null) {
            ItemStack[] contents = this.protocol.bukkit().getInventory().getContents();

            for (ItemStack item : contents) {
                if (item != null && item.getType() == MACE) {
                    return true;
                }
            }
        }
        return false;
    }

}
