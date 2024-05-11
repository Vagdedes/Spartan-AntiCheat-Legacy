package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPotionEffect;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class IrregularMovementsGravity extends DetectionExecutor {

    IrregularMovementsGravity(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(double vertical, double oldVertical, double box, double oldBox) {
        if (hackType.getCheck().getBooleanOption("check_gravity", true)) {
            int airTicks = player.movement.getTicksOnAir();

            runAsync(() -> { // todo ladder-drag/acceleration, slow-timer-bypass, web
                boolean liquid = player.movement.wasInLiquids();
                double prediction,
                        acceleration = liquid
                                ? PlayerUtils.liquidAcceleration
                                : player.hasPotionEffect(PotionEffectType.SLOW_FALLING, 0)
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
                    prediction = (oldVertical + ((0.05 * level - oldVertical) * 0.2)) * PlayerUtils.airDrag;
                } else {
                    prediction = PlayerUtils.calculateNextFallMotion(
                            oldVertical,
                            -acceleration,
                            drag
                    );
                }

                if (Math.abs(prediction) > PlayerUtils.optimizationY) {
                    double difference = Math.abs(vertical - prediction),
                            product = !player.movement.isJumping(vertical)
                                    && !player.movement.isFalling(vertical, acceleration, drag, GroundUtils.maxHeightLengthRatio)
                                    && !player.movement.isJumping(oldVertical)
                                    && !player.movement.isFalling(oldVertical, acceleration, drag, GroundUtils.maxHeightLengthRatio)
                                    ? 1.0
                                    : PlayerUtils.getJumpTicks(PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1),
                            limit = Math.max(
                                    product / (double) airTicks,
                                    player.getTrackers().has(Trackers.TrackerFamily.VELOCITY)
                                            ? GroundUtils.maxPossibleStep
                                            : liquid || player.getTrackers().has(Trackers.TrackerFamily.MOTION)
                                            ? GroundUtils.maxPlayerStep
                                            : player.bedrockPlayer ? PlayerUtils.jumpAcceleration :
                                            hasLevitation
                                                    ? 0.05
                                                    : GroundUtils.maxHeightLengthRatio
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
