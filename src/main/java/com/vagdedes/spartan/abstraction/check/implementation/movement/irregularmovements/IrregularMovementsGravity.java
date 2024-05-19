package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPotionEffect;
import com.vagdedes.spartan.functionality.server.TPS;
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
            SpartanPotionEffect levitationEffect = PlayerUtils.levitation
                    ? player.getPotionEffect(PotionEffectType.LEVITATION, 0)
                    : null;

            runAsync(() -> { // todo honey
                List<Double> predictions = new ArrayList<>(5);
                double acceleration = liquid
                        ? PlayerUtils.liquidAcceleration
                        : PlayerUtils.slowFall && player.hasPotionEffect(PotionEffectType.SLOW_FALLING, 0)
                        ? PlayerUtils.slowFallAcceleration
                        : PlayerUtils.airAcceleration,
                        drag = liquid
                                ? player.movement.getLastLiquidMaterial() == Material.LAVA
                                ? PlayerUtils.lavaDrag : PlayerUtils.waterDrag : PlayerUtils.airDrag;
                boolean levitation = levitationEffect != null,
                        climb = BlockUtils.canClimb(to.getBlock().material, false)
                                || BlockUtils.canClimb(from.getBlock().material, false),
                        web = BlockUtils.areWebs(to.getBlock().material)
                                || BlockUtils.areWebs(from.getBlock().material),
                        sensitive = web || climb || levitation;

                if (sensitive && !this.sensitive) {
                    this.ignore = true;
                }
                this.sensitive = sensitive;

                if (levitation) {
                    double level = levitationEffect.bukkitEffect.getAmplifier() + 1;
                    predictions.add(
                            (oldVertical + ((0.05 * level - oldVertical) * 0.2)) * PlayerUtils.airDrag
                    );
                } else {
                    predictions.add(
                            PlayerUtils.calculateNextFallMotion(
                                    oldVertical,
                                    -acceleration,
                                    drag
                            )
                    );
                }
                if (web) {
                    double webMovement = 0.06272;

                    if (levitation) {
                        predictions.set(0, predictions.get(0) - webMovement);
                    }
                    predictions.add(-webMovement);
                } else if (climb) {
                    if (vertical == 0.0) {
                        if (!ground && !sneaking && noNearbyEntities) {
                            cancel(
                                    "type: gravity(ladder-stable)"
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
                            product = !player.movement.isJumping(vertical) && !player.movement.isJumping(oldVertical)
                                    && !player.movement.isFalling(vertical, acceleration, drag, GroundUtils.maxHeightLengthRatio)
                                    && !player.movement.isFalling(oldVertical, acceleration, drag, GroundUtils.maxHeightLengthRatio)
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
                    if (!ignore) {
                        cancel(
                                violation,
                                airTicks / TPS.maximum,
                                player.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    }
                    ignore = false;
                }
            });
        }
    }

}
