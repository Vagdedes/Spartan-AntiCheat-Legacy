package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
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
    private static final int heightPrecision = 3;

    // Separator

    private long teleport;

    static {
        for (int jumpEffect = 0; jumpEffect < 255; jumpEffect++) {
            Set<Double>
                    collection = new HashSet<>(),
                    collectionBedrock = new HashSet<>();

            for (double height : GroundUtils.getHeights()) {
                for (double motion : PlayerUtils.getJumpMotions(jumpEffect)) {
                    height += motion;
                    collection.add(AlgebraUtils.cut(AlgebraUtils.removeBase(height), heightPrecision));
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
        super(executor, null, null, null, true);
        this.jump_ticks = new ImplementedProbabilityDetection(this, "jump_ticks", true);
        this.jump_height = new ImplementedProbabilityDetection(this, "jump_height", true);
    }

    void run(double vertical, double box) {
        if (this.teleport < System.currentTimeMillis()
                && (this.jump_ticks.canCall()
                || this.jump_height.canCall())
                && !this.protocol.bukkitExtra.movement.wasGliding()) {
            if (this.protocol.bukkitExtra.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY)
                    ? this.protocol.bukkitExtra.movement.justJumped(vertical)
                    : this.protocol.bukkitExtra.movement.isJumping(vertical)) {
                int jumpEffect = PlayerUtils.getPotionLevel(this.protocol.bukkitExtra, PotionEffectUtils.JUMP) + 1,
                        jumpTicks = PlayerUtils.getJumpTicks(jumpEffect);
                boolean contains = heights.get(jumpEffect).contains(AlgebraUtils.cut(box, heightPrecision));

                if (!contains && this.protocol.bukkitExtra.isBedrockPlayer()) {
                    for (double height : heightsBedrock.get(jumpEffect)) {
                        if (Math.abs(height - box) < heightPrecision) {
                            contains = true;
                            break;
                        }
                    }
                }

                if (!contains) {
                    this.jump_height.call(() -> {
                        int airTicks = this.protocol.bukkitExtra.movement.getTicksOnAir();

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
                                    this.protocol.getLocationOrVehicle(),
                                    true,
                                    GroundUtils.getAbstractMaterials()
                            )) {
                                this.jump_height.cancel("jump(height)"
                                                + ", box: " + box
                                                + ", vertical: " + vertical
                                                + ", jump-effect: " + jumpEffect
                                                + ", air-ticks: " + airTicks,
                                        this.protocol.getFromLocation(),
                                        0,
                                        true
                                );
                            }
                        }
                    });
                } else {
                    this.jump_ticks.call(() -> {
                        if (this.protocol.version == MultiVersion.MCVersion.OTHER
                                || this.protocol.isUsingVersionOrGreater(MultiVersion.serverVersion)) {
                            int limit = Math.max(jumpTicks, AlgebraUtils.integerCeil(TPS.maximum));

                            if (!this.protocol.isOnGround()
                                    && !this.protocol.isOnGroundFrom()
                                    && !this.protocol.bukkitExtra.movement.isInLiquids()) {
                                int airTicks = this.protocol.bukkitExtra.movement.getTicksOnAir();

                                if (airTicks > limit) {
                                    this.jump_ticks.cancel(
                                            "jump(ticks)"
                                                    + ", box: " + box
                                                    + ", vertical: " + vertical
                                                    + ", jump-effect: " + jumpEffect
                                                    + ", air-ticks: " + airTicks
                                                    + ", limit: " + limit,
                                            this.protocol.getFromLocation(),
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
    }

    void teleport() {
        this.teleport = System.currentTimeMillis() + 1_000L;
    }

}
