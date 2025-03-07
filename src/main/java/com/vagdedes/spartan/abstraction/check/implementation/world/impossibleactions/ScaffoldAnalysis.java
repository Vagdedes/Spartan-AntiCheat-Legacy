package com.vagdedes.spartan.abstraction.check.implementation.world.impossibleactions;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.java.OverflowMap;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScaffoldAnalysis extends CheckDetection {

    private final Map<Integer, Buffer.IndividualSimpleBuffer> buffers;
    private Block oldBlock;
    private long
            placeTime,
            placeHash;

    ScaffoldAnalysis(CheckRunner executor) {
        super(executor, null, null, "scaffold_analysis", true);
        this.buffers = new OverflowMap<>(new ConcurrentHashMap<>(2), 124);
        this.placeTime = System.currentTimeMillis();
        this.placeHash = 0;
    }

    void run(Block currentBlock) {
        this.call(() -> {
            PlayerProtocol p = this.protocol;

            if (this.oldBlock == null) {
                this.oldBlock = currentBlock;
                return;
            }
            Location l = p.bukkitExtra.getLocation();
            Location lB = new Location(l.getWorld(), (long) l.getX(), (long) l.getY(), (long) l.getZ());
            Location lB8 = new Location(l.getWorld(), (long) l.getX() + 1, (long) l.getY(), (long) l.getZ());
            long placeHash = (long) ((lB.getX() * 31) + lB.getZ());
            Buffer.IndividualSimpleBuffer buffer = buffers.computeIfAbsent(
                    101, k -> new Buffer.IndividualSimpleBuffer());
            Location bL = currentBlock.getLocation();
            Block oldB = this.oldBlock;
            Material downBType = new SpartanLocation(
                    bL.clone().add(0, -1, 0)
            ).getBlock().getTypeOrNull();
            {
                long delay = System.currentTimeMillis() - this.placeTime;
                boolean checkHash = false;
                boolean flag = true;
                {
                    List<Long> hashList = Arrays.asList(
                            (long) ((bL.getX() * 31) + bL.getZ()),
                            (long) (((bL.getX() + 0.3) * 31) + bL.getZ()),      // +0.3 X, 0 Z
                            (long) (((bL.getX() - 0.3) * 31) + bL.getZ()),      // -0.3 X, 0 Z
                            (long) ((bL.getX() * 31) + (bL.getZ() + 0.3)),      // 0 X, +0.3 Z
                            (long) ((bL.getX() * 31) + (bL.getZ() - 0.3)),      // 0 X, -0.3 Z
                            (long) (((bL.getX() + 0.3) * 31) + (bL.getZ() + 0.3)),  // +0.3 X, +0.3 Z
                            (long) (((bL.getX() + 0.3) * 31) + (bL.getZ() - 0.3)),  // +0.3 X, -0.3 Z
                            (long) (((bL.getX() - 0.3) * 31) + (bL.getZ() + 0.3)),  // -0.3 X, +0.3 Z
                            (long) (((bL.getX() - 0.3) * 31) + (bL.getZ() - 0.3))   // -0.3 X, -0.3 Z
                    );
                    for (Long v : hashList) {
                        if (p.bukkitExtra.isOnGround()) {
                            if (v == placeHash || SpartanLocation.distance(bL, l) < 1.2) {
                                checkHash = true;
                                break;
                            } else if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                                Vector v2 = lB.toVector().clone();
                                Vector v1 = bL.toVector().clone();
                                if (v1.getX() == v2.getX() - 1 && v1.getZ() == v2.getZ()) {
                                    checkHash = true;
                                    break;
                                }
                            }
                        } else if (SpartanLocation.distance(bL, l) < 2.35) {
                            checkHash = true;
                            break;
                        }
                    }
                }
                if (delay < 5
                        || placeHash == this.placeHash
                        || (long) p.bukkitExtra.getLocation().getY() <= bL.getY()
                        || p.bukkit().isFlying() || !checkHash) {
                    p.rightClickCounter = 0;
                    this.placeTime = System.currentTimeMillis();
                    this.oldBlock = currentBlock;
                /*
                p.player.sendMessage("c 1 " + (delay < 5)
                                + " " + (placeHash == p.placeHash) + " "
                                + ((long) p.bukkitExtra.getLocation().getY() <= bL.getY())
                                + " " + p.player.isFlying() + " " + !checkHash);
                 */
                    return;
                } else if (bL.getY() != oldB.getLocation().getY()
                        || !BlockUtils.areAir(downBType)) {
                    p.rightClickCounter = 0;
                    this.oldBlock = currentBlock;
                    if (buffer.count > 5) buffer.count -= 5;
                    return;
                }

                if (delay < 280) {
                    buffer.count += 4;
                } else if (delay < (p.bukkitExtra.isSneaking() ? 345 : 385) && p.rightClickCounter < 3) {
                    buffer.count += 2;
                } else {
                    flag = false;
                }

                if (buffer.count > 12 && flag) {
                    this.cancel("scaffold analysis ("
                            + (long) Math.abs((((double) 400 / (double) delay) * 100) - 100)
                            + "% faster than possible) [VL: " + buffer.count + "]");
                    buffer.count = 10;
                } else if (buffer.count > 0) {
                    buffer.count -= 2;
                }

            }

            this.oldBlock = currentBlock;
            this.placeHash = placeHash;
            this.placeTime = System.currentTimeMillis();
            p.rightClickCounter = 0;
        });
    }

}
