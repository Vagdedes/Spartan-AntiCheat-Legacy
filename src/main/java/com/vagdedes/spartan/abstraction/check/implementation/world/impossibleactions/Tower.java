package com.vagdedes.spartan.abstraction.check.implementation.world.impossibleactions;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.OverflowMap;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PotionEffectUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Tower extends ProbabilityDetection {

    private final Map<Integer, Buffer.IndividualBuffer> buffers;

    Tower(CheckRunner executor) {
        super(executor, "tower", true);
        this.buffers = new OverflowMap<>(new ConcurrentHashMap<>(2), 124);
    }

    final void run(Block block,
                   Location playerLocation,
                   int playerY,
                   int blockY) {
        if (this.protocol.packetsEnabled()) {
            return;
        }
        Double deltaXZ = AlgebraUtils.getHorizontalDistance(this.protocol.getLocation(), this.protocol.getFromLocation());

        if (deltaXZ == null) {
            return;
        }
        if (AlgebraUtils.getHorizontalDistance(
                SpartanLocation.getBlockLocation(playerLocation),
                block.getLocation()
        ) <= 1.0) {
            ItemStack handItem = this.protocol.spartan.getItemInHand();
            int jumpLevel = PlayerUtils.getPotionLevel(this.protocol.spartan, PotionEffectUtils.JUMP) + 1,
                    motionTicks = PlayerUtils.getJumpTicks(jumpLevel);
            double motionSum = PlayerUtils.getJumpMotionSum(jumpLevel),
                    blockHeight = GroundUtils.getMinHeight(
                            handItem == null ? Material.AIR : handItem.getType()
                    );
            int deltaYMinus1 = playerY - blockY,
                    possiblePlacements = AlgebraUtils.integerFloor(
                            motionSum / (blockHeight == 0.0 ? 1.0 : blockHeight)
                    ) + deltaYMinus1;

            if (deltaXZ > 0.1) {
                possiblePlacements += AlgebraUtils.integerCeil(deltaXZ / 0.25);
            }
            possiblePlacements = Math.max(possiblePlacements, 2);
            Buffer.IndividualBuffer buffer = this.buffers.computeIfAbsent(
                    (block.getX() * SpartanBukkit.hashCodeMultiplier) + block.getZ(),
                    k -> new Buffer.IndividualBuffer()
            );

            if (buffer.count(1, motionTicks) > possiblePlacements) {
                buffer.reset();
                this.cancel(
                        "type: tower"
                                + ", placements: " + possiblePlacements
                                + ", horizontal: " + deltaXZ
                                + ", vertical: " + deltaYMinus1,
                        this.protocol.getFromLocation(),
                        0,
                        true
                );
            }
        }
    }

}
