package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanLocation;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.server.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.GroundUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class IrregularMovementsGravity extends DetectionExecutor {

    private boolean ignore, sensitive;

    IrregularMovementsGravity(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        this.ignore = false;
        this.sensitive = false;
    }

    void run(double vertical, double oldVertical, double box, double oldBox) {
        if (hackType.getCheck().getBooleanOption("check_gravity", true)) {
            int airTicks = player.movement.getTicksOnAir();
            boolean liquid = player.movement.wasInLiquids(),
                    velocity = player.trackers.has(Trackers.TrackerFamily.VELOCITY),
                    motion = player.trackers.has(Trackers.TrackerFamily.MOTION),
                    ground = player.isOnGround(),
                    sneaking = player.movement.isSneaking(),
                    noNearbyEntities = player.getNearbyEntities(1.0, 1.0, 1.0).isEmpty();
            SpartanLocation to = player.movement.getEventToLocation(),
                    from = player.movement.getEventFromLocation();
            int levitationLevel = PlayerUtils.levitation
                    ? PlayerUtils.getPotionLevel(player, PotionEffectType.LEVITATION)
                    : -1;

            runAsync(() -> {
                List<Double> predictions = new ArrayList<>(5);
                double acceleration = liquid
                        ? PlayerUtils.liquidAcceleration
                        : PlayerUtils.slowFall && player.hasPotionEffect(PotionEffectType.SLOW_FALLING, 0)
                        ? PlayerUtils.slowFallAcceleration
                        : PlayerUtils.airAcceleration,
                        drag = liquid
                                ? player.movement.getLastLiquidMaterial() == Material.LAVA
                                ? PlayerUtils.lavaDrag : PlayerUtils.waterDrag : PlayerUtils.airDrag;
                boolean levitation = levitationLevel != -1,
                        climb = BlockUtils.canClimb(to.getBlock().material, false)
                                || BlockUtils.canClimb(from.getBlock().material, false),
                        web = BlockPatternUtils.isBlockPattern(
                                new double[][]{
                                        {GroundUtils.contactBoundingBox, 0.0, GroundUtils.contactBoundingBox},
                                        {GroundUtils.contactBoundingBox, 1.0, GroundUtils.contactBoundingBox},
                                        {GroundUtils.contactBoundingBox, Math.ceil(player.getEyeHeight()), GroundUtils.contactBoundingBox}
                                },
                                to,
                                true,
                                BlockUtils.web
                        ) || BlockPatternUtils.isBlockPattern(
                                new double[][]{
                                        {GroundUtils.contactBoundingBox, 0.0, GroundUtils.contactBoundingBox},
                                        {GroundUtils.contactBoundingBox, 1.0, GroundUtils.contactBoundingBox},
                                        {GroundUtils.contactBoundingBox, Math.ceil(player.getEyeHeight()), GroundUtils.contactBoundingBox}
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
                                -acceleration,
                                drag
                        )
                );

                if (web) {
                    if (levitation) {
                        predictions.set(0, predictions.get(0) - PlayerUtils.webBlockDownDefault);
                    }
                    predictions.add(-PlayerUtils.webBlockDownDefault);
                } else if (climb) {
                    if (vertical == 0.0) {
                        if (!ground && !sneaking && noNearbyEntities
                                && !BlockPatternUtils.isBlockPattern(
                                new double[]{
                                        GroundUtils.boundingBox,
                                        Math.ceil(player.getEyeHeight()),
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
                } else if (honey) {
                    if (levitation) {
                        predictions.set(0, predictions.get(0) - PlayerUtils.honeyBlockDownDefault);
                    }
                    predictions.add(-PlayerUtils.honeyBlockDownDefault);
                }
                boolean sensitive = predictions.size() > 1;

                if (sensitive && !this.sensitive) {
                    this.ignore = true;
                }
                this.sensitive = sensitive;
                String violation = null;

                for (double prediction : predictions) {
                    if (Math.abs(prediction) > PlayerUtils.optimizationY) {
                        double difference = Math.abs(vertical - prediction), product;

                        if (player.bedrockPlayer) {
                            product = PlayerUtils.calculateTerminalVelocity(
                                    PlayerUtils.airDrag,
                                    PlayerUtils.airAcceleration
                            );
                        } else {
                            product = !sensitive
                                    && !player.movement.isJumping(vertical) && !player.movement.isJumping(oldVertical)
                                    && !player.movement.isFalling(vertical, -acceleration, drag, GroundUtils.maxHeightLengthRatio)
                                    && !player.movement.isFalling(oldVertical, -acceleration, drag, GroundUtils.maxHeightLengthRatio)
                                    ? 1.0
                                    : PlayerUtils.getJumpTicks(PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1);
                        }
                        double limit = Math.max(
                                product / (double) airTicks,
                                velocity ? GroundUtils.maxPossibleStep :
                                        liquid || motion ? GroundUtils.maxPlayerStep :
                                                player.bedrockPlayer || sensitive ? PlayerUtils.climbingDownDefault :
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
                                    + ", air-ticks: " + airTicks;
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
            });
        }
    }

}
