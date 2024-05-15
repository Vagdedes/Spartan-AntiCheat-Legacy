package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.GroundUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IrregularMovementsJump extends DetectionExecutor {

    private static final Map<Integer, Set<Double>> heights = new LinkedHashMap<>();

    static {
        for (int jumpEffect = 0; jumpEffect < 255; jumpEffect++) {
            Set<Double> collection = new HashSet<>();

            for (double height : GroundUtils.getHeights()) {
                for (double motion : PlayerUtils.getJumpMotions(jumpEffect)) {
                    height += motion;
                    collection.add(AlgebraUtils.cut(AlgebraUtils.removeBase(height), MovementProcessing.heightPrecision));
                }
            }
            heights.put(jumpEffect, collection);
        }
    }

    IrregularMovementsJump(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(double vertical, double box) {
        if (hackType.getCheck().getBooleanOption("check_jump", true)) {
            int airTicks = player.movement.getTicksOnAir();

            runAsync(() -> {
                if (player.getTrackers().has(Trackers.TrackerFamily.VELOCITY)
                        ? player.movement.justJumped(vertical)
                        : player.movement.isJumping(vertical)) {
                    int jumpEffect = PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1;

                    if (!heights.get(jumpEffect).contains(AlgebraUtils.cut(box, MovementProcessing.heightPrecision))) {
                        cancel("type: jump(height)"
                                        + ", box: " + box
                                        + ", vertical: " + vertical
                                        + ", jump-effect: " + jumpEffect
                                        + ", air-ticks: " + airTicks,
                                airTicks / TPS.tickTimeDecimal,
                                player.movement.getDetectionLocation(),
                                0,
                                true
                        );
                    } else {
                        double limit = Math.max(PlayerUtils.getJumpTicks(jumpEffect), TPS.maximum);

                        if (!player.isOnGround()
                                && !player.movement.wasInLiquids()
                                && airTicks > limit
                                && !player.isOnGround(player.movement.getEventFromLocation())) {
                            cancel("type: jump(ticks)"
                                            + ", box: " + box
                                            + ", vertical: " + vertical
                                            + ", jump-effect: " + jumpEffect
                                            + ", air-ticks: " + airTicks
                                            + ", limit: " + limit,
                                    (airTicks / limit) - 1.0,
                                    player.movement.getDetectionLocation(),
                                    0,
                                    true
                            );
                        }
                    }
                }
            });
        }
    }

}
