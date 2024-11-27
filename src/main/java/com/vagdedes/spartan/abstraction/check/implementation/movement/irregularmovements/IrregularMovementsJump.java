package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PotionEffectUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IrregularMovementsJump extends ProbabilityDetection {

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

    // Separator

    private final CheckDetection
            jump_ticks,
            jump_height;

    IrregularMovementsJump(CheckRunner executor) {
        super(executor, null, true);
        this.jump_ticks = new ImplementedProbabilityDetection(this, "jump_ticks", true);
        this.jump_height = new ImplementedProbabilityDetection(this, "jump_height", true);
    }

    void run(double vertical, double box) {
        if (this.protocol().spartan.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY)
                ? this.protocol().spartan.movement.justJumped(vertical)
                : this.protocol().spartan.movement.isJumping(vertical)) {
            int jumpEffect = PlayerUtils.getPotionLevel(this.protocol().spartan, PotionEffectUtils.JUMP) + 1,
                    jumpTicks = PlayerUtils.getJumpTicks(jumpEffect);
            boolean contains = heights.get(jumpEffect).contains(AlgebraUtils.cut(box, MovementProcessing.heightPrecision));

            if (!contains && this.protocol().spartan.isBedrockPlayer()) {
                for (double height : heightsBedrock.get(jumpEffect)) {
                    if (Math.abs(height - box) < MovementProcessing.heightPrecision) {
                        contains = true;
                        break;
                    }
                }
            }

            if (!contains) {
                int airTicks = this.protocol().spartan.movement.getTicksOnAir();

                if (airTicks > jumpTicks) {
                    int blocks = AlgebraUtils.integerCeil(PlayerUtils.getJumpMotionSum(jumpEffect));
                    double[][] patterns = new double[blocks][3];

                    for (int current = 1; current <= blocks; current++) {
                        int position = current - 1;
                        patterns[position][0] = GroundUtils.boundingBox;
                        patterns[position][1] = -current;
                        patterns[position][2] = GroundUtils.boundingBox;
                    }
                    if (!BlockPatternUtils.isBlockPattern(
                            patterns,
                            this.protocol().spartan.movement.getLocation(),
                            true,
                            GroundUtils.abstractMaterials
                    )) {
                        this.jump_height.cancel("type: jump(height)"
                                        + ", box: " + box
                                        + ", vertical: " + vertical
                                        + ", jump-effect: " + jumpEffect
                                        + ", air-ticks: " + airTicks,
                                this.protocol().getFromLocation(),
                                0,
                                true
                        );
                    }
                }
            } else if (this.protocol().version == MultiVersion.MCVersion.OTHER
                    || this.protocol().isUsingVersionOrGreater(MultiVersion.serverVersion)) {
                int limit = Math.max(jumpTicks, AlgebraUtils.integerCeil(TPS.maximum));

                if (!this.protocol().spartan.isOnGround(this.protocol().spartan.movement.getLocation(), true)
                        && !this.protocol().spartan.isOnGround(this.protocol().spartan.movement.getEventFromLocation(), false)
                        && !this.protocol().spartan.movement.isInLiquids()) {
                    int airTicks = this.protocol().spartan.movement.getTicksOnAir();

                    if (airTicks > limit) {
                        this.jump_ticks.cancel(
                                "type: jump(ticks)"
                                        + ", box: " + box
                                        + ", vertical: " + vertical
                                        + ", jump-effect: " + jumpEffect
                                        + ", air-ticks: " + airTicks
                                        + ", limit: " + limit,
                                this.protocol().getFromLocation(),
                                0,
                                true
                        );
                    }
                }
            }
        }
    }

}
