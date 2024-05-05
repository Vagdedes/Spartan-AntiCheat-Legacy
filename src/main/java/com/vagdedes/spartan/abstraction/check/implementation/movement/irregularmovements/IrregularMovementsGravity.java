package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
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
            runAsync(() -> { // todo ladder-drag/acceleration, levitation effect, slow-fall effect, velocity, slow-timer-bypass
                boolean liquid = player.movement.wasInLiquids();
                double prediction = PlayerUtils.calculateNextFallMotion(
                        oldVertical,
                        liquid ? (player.movement.getLastLiquidMaterial() == Material.LAVA
                                ? PlayerUtils.lavaDrag : PlayerUtils.waterDrag)
                                : PlayerUtils.airDrag,
                        liquid ? PlayerUtils.liquidAcceleration : PlayerUtils.airAcceleration,
                        -(PlayerUtils.terminalVelocity - GroundUtils.maxHeightLengthRatio)
                );

                if (Math.abs(prediction) > PlayerUtils.optimizationY) {
                    double difference = Math.abs(vertical - prediction),
                            product = !player.movement.isJumping(vertical) && !PlayerUtils.isFalling(vertical)
                                    && !player.movement.isJumping(oldVertical) && !PlayerUtils.isFalling(oldVertical)
                                    ? 1.0
                                    : PlayerUtils.getJumpTicks(PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1),
                            limit = Math.max(
                                    product / (double) player.movement.getTicksOnAir(),
                                    player.getTrackers().has(Trackers.TrackerFamily.Velocity)
                                            ? GroundUtils.maxPossibleStep :
                                            liquid || player.getTrackers().has(Trackers.TrackerFamily.Motion)
                                                    ? GroundUtils.maxPlayerStep :
                                                    player.bedrockPlayer ? PlayerUtils.jumpAcceleration :
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
                                        + ", air-ticks: " + player.movement.getTicksOnAir(),
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
