package com.vagdedes.spartan.abstraction.check.implementation.world.impossibleactions;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.utils.java.OverflowMap;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PotionEffectUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Tower extends CheckDetection {

    private final Map<Integer, Buffer.IndividualBuffer> buffers;

    Tower(CheckRunner executor) {
        super(executor, null, Check.DetectionType.BUKKIT, "tower", true);
        this.buffers = new OverflowMap<>(new ConcurrentHashMap<>(2), 124);
    }

    void run(Block block,
             Location playerLocation,
             int playerY,
             int blockY) {
        this.call(() -> {
            Double deltaXZ = AlgebraUtils.getHorizontalDistance(this.protocol.bukkitExtra.getLocation(), this.protocol.getFromLocation());

            if (deltaXZ == null) {
                return;
            }
            if (AlgebraUtils.getHorizontalDistance(
                    SpartanLocation.getBlockLocation(playerLocation),
                    block.getLocation()
            ) <= 1.0) {
                int jumpLevel = PlayerUtils.getPotionLevel(this.protocol, PotionEffectUtils.JUMP) + 1,
                        motionTicks = PlayerUtils.getJumpTicks(jumpLevel);
                double motionSum = PlayerUtils.getJumpMotionSum(jumpLevel),
                        blockHeight = (GroundUtils.minBoundingBox + GroundUtils.maxBoundingBox) / 2.0;
                int deltaYMinus1 = playerY - blockY,
                        possiblePlacements = AlgebraUtils.integerFloor(
                                motionSum / blockHeight
                        ) + deltaYMinus1;

                if (deltaXZ > 0.1) {
                    possiblePlacements += AlgebraUtils.integerCeil(deltaXZ / 0.25);
                }
                possiblePlacements = Math.max(possiblePlacements, 2);
                Buffer.IndividualBuffer buffer = this.buffers.computeIfAbsent(
                        (block.getX() * PluginBase.hashCodeMultiplier) + block.getZ(),
                        k -> new Buffer.IndividualBuffer()
                );

                if (buffer.count(1, motionTicks) > possiblePlacements) {
                    buffer.reset();
                    this.cancel(
                            "tower"
                                    + ", placements: " + possiblePlacements
                                    + ", horizontal: " + deltaXZ
                                    + ", vertical: " + deltaYMinus1,
                            this.protocol.getFromLocation(),
                            0,
                            true
                    );
                }
            }
        });
    }

}
