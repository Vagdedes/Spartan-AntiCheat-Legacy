package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import org.bukkit.Material;

public class IrregularMovementsGravity extends DetectionExecutor {

    IrregularMovementsGravity(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(double vertical, double oldVertical, double box, double oldBox) {
        if (hackType.getCheck().getBooleanOption("check_gravity", true)) {
            runAsync(() -> { // todo ladder, levitation, slow-fall
                boolean liquid = player.movement.getLastLiquid() == 0L;
                double prediction = PlayerUtils.calculateNextFallMotion(
                        oldVertical,
                        liquid && player.movement.getLastLiquidMaterial() == Material.WATER ? PlayerUtils.waterDrag :
                                liquid && player.movement.getLastLiquidMaterial() == Material.LAVA ? PlayerUtils.lavaDrag :
                                        PlayerUtils.airDrag,
                        liquid ? PlayerUtils.liquidAcceleration : PlayerUtils.airAcceleration,
                        PlayerUtils.terminalVelocity - GroundUtils.maxHeightLengthRatio
                );

                if (Math.abs(prediction) > PlayerUtils.optimizationY) {
                    double difference = Math.abs(vertical - prediction),
                            limit = Math.max(
                                    PlayerUtils.getJumpTicks(0) / (double) player.movement.getTicksOnAir(),
                                    liquid || player.getTrackers().has(Trackers.TrackerFamily.Velocity)
                                            || player.getTrackers().has(Trackers.TrackerFamily.Motion)
                                            ? GroundUtils.maxPlayerStep :
                                            player.bedrockPlayer ? PlayerUtils.jumpAcceleration :
                                                    GroundUtils.maxHeightLengthRatio
                            );

                    if (difference >= limit) {
                        cancel(
                                "type: gravity"
                                        + ", limit: " + limit
                                        + ", difference: " + difference
                                        + ", prediction: " + prediction
                                        + ", vertical: " + vertical
                                        + ", old-vertical: " + oldVertical
                                        + ", box: " + box
                                        + ", old-box: " + oldBox,
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
