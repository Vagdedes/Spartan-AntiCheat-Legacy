package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.player.PlayerTrackers;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
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

public class IrregularMovementsGravity extends DetectionExecutor {

    private static final Material MACE = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)
            ? MaterialUtils.findMaterial("MACE")
            : null;

    private boolean ignore, sensitive;

    IrregularMovementsGravity(CheckExecutor executor) {
        super(executor, "gravity", true);
    }

    void run(double vertical, double oldVertical, double box, double oldBox) {
        if (!isUsingMace()) {
            int airTicks = player.movement.getTicksOnAir();
            SpartanLocation to = player.movement.getLocation(),
                    from = player.movement.getEventFromLocation();
            int levitationLevel = PlayerUtils.levitation
                    ? PlayerUtils.getPotionLevel(player, PotionEffectType.LEVITATION)
                    : -1;
            List<Double> predictions = new ArrayList<>(5);
            boolean levitation = levitationLevel != -1,
                    slowFall = PlayerUtils.slowFall && player.hasPotionEffect(PotionEffectType.SLOW_FALLING, 0),
                    liquid = player.movement.isInLiquids() || player.movement.isInLiquids(),
                    climb = player.isFrozen() || canClimb(to) || canClimb(from),
                    web = BlockPatternUtils.isBlockPattern(
                            new double[][]{
                                    {GroundUtils.contactBoundingBox, 0.0, GroundUtils.contactBoundingBox},
                                    {GroundUtils.contactBoundingBox, 1.0, GroundUtils.contactBoundingBox},
                                    {GroundUtils.contactBoundingBox, Math.ceil(player.getInstance().getEyeHeight()), GroundUtils.contactBoundingBox}
                            },
                            to,
                            true,
                            BlockUtils.web
                    ) || BlockPatternUtils.isBlockPattern(
                            new double[][]{
                                    {GroundUtils.contactBoundingBox, 0.0, GroundUtils.contactBoundingBox},
                                    {GroundUtils.contactBoundingBox, 1.0, GroundUtils.contactBoundingBox},
                                    {GroundUtils.contactBoundingBox, Math.ceil(player.getInstance().getEyeHeight()), GroundUtils.contactBoundingBox}
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
                            -player.movement.getAirAcceleration(),
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
                        player.movement.getLastLiquidMaterial() == Material.LAVA
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
                    if (!player.isOnGround(to, true)
                            && !player.protocol.isSneaking()
                            && player.getNearbyEntities(1.0, 1.0, 1.0).isEmpty()
                            && !BlockPatternUtils.isBlockPattern(
                            new double[]{
                                    GroundUtils.boundingBox,
                                    Math.ceil(player.getInstance().getEyeHeight()),
                                    GroundUtils.boundingBox
                            },
                            to,
                            true,
                            BlockUtils.solid, BlockUtils.semi_solid
                    )) {
                        cancel(
                                "type: gravity(climb-stable)"
                                        + ", vertical: " + vertical
                                        + ", old-vertical: " + oldVertical
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", air-ticks: " + airTicks,
                                1.0,
                                player.movement.getDetectionLocation(),
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

                    if (player.isBedrockPlayer()) {
                        product = PlayerUtils.calculateTerminalVelocity(
                                PlayerUtils.airDrag,
                                player.movement.getAirAcceleration()
                        ) * 2;
                    } else {
                        product = !sensitive
                                && !player.movement.isJumping(vertical) && !player.movement.isJumping(oldVertical)
                                && !player.movement.isFalling(vertical, -player.movement.getAirAcceleration(), PlayerUtils.airDrag, GroundUtils.maxHeightLengthRatio)
                                && !player.movement.isFalling(oldVertical, -player.movement.getAirAcceleration(), PlayerUtils.airDrag, GroundUtils.maxHeightLengthRatio)
                                ? GroundUtils.maxPlayerStep
                                : PlayerUtils.getJumpTicks(PlayerUtils.getPotionLevel(player, PotionEffectUtils.JUMP) + 1);
                    }
                    double limit = Math.max(
                            this.sensitive ? product : product / (double) airTicks,
                            player.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY)
                                    || player.trackers.has(PlayerTrackers.TrackerFamily.MOTION) ? GroundUtils.maxBoundingBox :
                                    liquid ? GroundUtils.maxPlayerStep :
                                            player.isBedrockPlayer() || sensitive ? PlayerUtils.climbingDownDefault :
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
                    cancel(
                            violation,
                            sensitive ? 1.0 : airTicks / TPS.maximum,
                            player.movement.getDetectionLocation(),
                            0,
                            true
                    );
                }
            }
        }
    }

    private boolean canClimb(SpartanLocation location) {
        Material material = location.getBlock().getType();
        return BlockUtils.canClimb(material, false)
                || BlockUtils.areTrapdoors(material)
                && (BlockUtils.canClimb(location.clone().add(0, 1, 0).getBlock().getType(), false)
                || BlockUtils.canClimb(location.clone().add(0, -1, 0).getBlock().getType(), false));
    }

    private boolean isUsingMace() {
        if (MACE != null) {
            ItemStack[] contents = player.getInstance().getInventory().getContents();

            for (ItemStack item : contents) {
                if (item != null && item.getType() == MACE) {
                    return true;
                }
            }
        }
        return false;
    }

}
