package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPotionEffect;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.server.GroundUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class IrregularMovementsGravity extends DetectionExecutor {

    private static final double levitationMultiplier = 0.05;

    IrregularMovementsGravity(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(double vertical, double oldVertical, double box, double oldBox) {
        if (hackType.getCheck().getBooleanOption("check_gravity", true)) {
            int airTicks = player.movement.getTicksOnAir();
            boolean liquid = player.movement.wasInLiquids(),
                    velocity = player.trackers.has(Trackers.TrackerFamily.VELOCITY),
                    motion = player.trackers.has(Trackers.TrackerFamily.MOTION);

            runAsync(() -> { // todo ladder-drag/acceleration, slow-timer-bypass, web, honey
                double prediction,
                        acceleration = liquid
                                ? PlayerUtils.liquidAcceleration
                                : PlayerUtils.slowFall && player.hasPotionEffect(PotionEffectType.SLOW_FALLING, 0)
                                ? PlayerUtils.slowFallAcceleration
                                : PlayerUtils.airAcceleration,
                        drag = liquid
                                ? player.movement.getLastLiquidMaterial() == Material.LAVA
                                ? PlayerUtils.lavaDrag : PlayerUtils.waterDrag : PlayerUtils.airDrag;
                SpartanPotionEffect levitation = PlayerUtils.levitation
                        ? player.getPotionEffect(PotionEffectType.LEVITATION, 0)
                        : null;
                boolean hasLevitation = levitation != null;

                if (hasLevitation) {
                    double level = levitation.bukkitEffect.getAmplifier() + 1;
                    prediction = (oldVertical + ((levitationMultiplier * level - oldVertical) * 0.2)) * PlayerUtils.airDrag;
                } else {
                    prediction = PlayerUtils.calculateNextFallMotion(
                            oldVertical,
                            -acceleration,
                            drag
                    );
                }

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
                                            player.bedrockPlayer ? PlayerUtils.jumpAcceleration :
                                                    hasLevitation ? levitationMultiplier :
                                                            GroundUtils.maxHeightLengthRatio
                    );

                    if (difference >= limit) {
                        cancel(
                                "type: gravity"
                                        + ", product: " + product
                                        + ", limit: " + limit
                                        + ", difference: " + difference
                                        + ", prediction: " + prediction
                                        + ", vertical: " + vertical
                                        + ", old-vertical: " + oldVertical
                                        + ", box: " + box
                                        + ", old-box: " + oldBox
                                        + ", air-ticks: " + airTicks,
                                airTicks / TPS.maximum,
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
