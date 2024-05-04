package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class IrregularMovementsGravity extends DetectionExecutor {

    IrregularMovementsGravity(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(double vertical, double oldVertical, double box, double oldBox) {
        if (hackType.getCheck().getBooleanOption("check_gravity", true)) {
            SpartanLocation location = player.movement.getNmsToLocation();

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
                    boolean collision = PatternUtils.isBlockPattern( // todo fix
                            new double[][]{
                                    {
                                            BlockUtils.boundingBox,
                                            prediction > 0.0
                                                    ? Math.ceil(player.getEyeHeight())
                                                    : prediction,
                                            BlockUtils.boundingBox
                                    },
                                    {
                                            BlockUtils.boundingBox,
                                            prediction > 0.0
                                                    ? Math.ceil(player.getEyeHeight()) + Math.ceil(prediction)
                                                    : prediction,
                                            BlockUtils.boundingBox
                                    }
                            },
                            location,
                            true,
                            BlockUtils.solid, BlockUtils.semi_solid
                    );
                    double difference = Math.abs(vertical - prediction),
                            limit = Math.max(
                                    PlayerUtils.getJumpMotionSum(PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1)
                                            / (double) player.movement.getTicksOnAir(),
                                    player.getTrackers().has(Trackers.TrackerFamily.Velocity)
                                            ? GroundUtils.maxPossibleStep :
                                            liquid || collision || player.getTrackers().has(Trackers.TrackerFamily.Motion)
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
