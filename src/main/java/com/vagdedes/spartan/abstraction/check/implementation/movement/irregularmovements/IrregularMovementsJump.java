package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.server.GroundUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IrregularMovementsJump extends DetectionExecutor {

    private static final Map<Integer, Set<Double>>
            heights = new LinkedHashMap<>(),
            heightsBedrock = new LinkedHashMap<>();

    static {
        for (int jumpEffect = 0; jumpEffect < 255; jumpEffect++) {
            Set<Double>
                    collection = new HashSet<>(),
                    collectionBedrock = new HashSet<>();

            for (double height : GroundUtils.getHeights()) {
                for (double motion : PlayerUtils.getJumpMotions(jumpEffect)) {
                    height += motion;
                    collection.add(AlgebraUtils.cut(AlgebraUtils.removeBase(height), MovementProcessing.heightPrecision));
                    collectionBedrock.add(AlgebraUtils.removeBase(height));
                }
            }
            heights.put(jumpEffect, collection);
            heightsBedrock.put(jumpEffect, collectionBedrock);
        }
    }

    IrregularMovementsJump(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run(double vertical, double box) {
        if (hackType.getCheck().getBooleanOption("check_jump", true)) {
            SpartanLocation to = player.movement.getEventToLocation();
            int airTicks = player.movement.getTicksOnAir(),
                    jumpEffect = PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1;
            boolean liquids = player.movement.wasInLiquids(),
                    toGround = player.isOnGround(),
                    fromGround = player.isOnGround(player.movement.getEventFromLocation(), true),
                    velocity = player.trackers.has(Trackers.TrackerFamily.VELOCITY);

            runAsync(() -> {
                if (velocity
                        ? player.movement.justJumped(vertical)
                        : player.movement.isJumping(vertical)) {
                    boolean contains = heights.get(jumpEffect).contains(AlgebraUtils.cut(box, MovementProcessing.heightPrecision));

                    if (!contains && player.bedrockPlayer) {
                        for (double height : heightsBedrock.get(jumpEffect)) {
                            if (Math.abs(height - box) < MovementProcessing.heightPrecision) {
                                contains = true;
                                break;
                            }
                        }
                    }

                    if (!contains) {
                        int blocks = AlgebraUtils.integerCeil(PlayerUtils.getJumpMotionSum(jumpEffect));
                        double[][] patterns = new double[blocks][3];

                        for (int current = 1; current <= blocks; current++) {
                            int position = AlgebraUtils.integerFloor(current);
                            patterns[position][0] = GroundUtils.boundingBox;
                            patterns[position][1] = -current;
                            patterns[position][2] = GroundUtils.boundingBox;
                        }
                        if (!BlockPatternUtils.isBlockPattern(
                                patterns,
                                to,
                                true,
                                GroundUtils.abstractMaterials
                        )) {
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
                        }
                    } else {
                        double limit = Math.max(PlayerUtils.getJumpTicks(jumpEffect), TPS.maximum);

                        if (!toGround
                                && !fromGround
                                && !liquids
                                && airTicks > limit) {
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
