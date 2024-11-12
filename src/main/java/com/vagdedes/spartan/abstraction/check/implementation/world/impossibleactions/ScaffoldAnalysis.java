package com.vagdedes.spartan.abstraction.check.implementation.world.impossibleactions;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.java.OverflowMap;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScaffoldAnalysis extends ProbabilityDetectionExecutor {

    private final Map<Integer, Buffer.IndividualSimpleBuffer> buffers;

    ScaffoldAnalysis(CheckExecutor executor) {
        super(executor, "scaffold_analysis", true);
        this.buffers = new OverflowMap<>(new LinkedHashMap<>(), 124);
    }

    void run(BlockPlaceEvent e) {
        SpartanProtocol p = this.protocol();

        if (p.oldBlockEvent == null) {
            p.oldBlockEvent = e;
            return;
        }
        Location l = p.getLocation();
        Location lB = new Location(l.getWorld(), (long) l.getX(), (long) l.getY(), (long) l.getZ());
        Location lB8 = new Location(l.getWorld(), (long) l.getX() + 1, (long) l.getY(), (long) l.getZ());
        long placeHash = (long) ((lB.getX() * 31) + lB.getZ());
        Buffer.IndividualSimpleBuffer buffer = buffers.computeIfAbsent(
                101, k -> new Buffer.IndividualSimpleBuffer());
        Block b = e.getBlock();
        Location bL = b.getLocation();
        Block oldB = p.oldBlockEvent.getBlock();
        Block downB = p.bukkit.getWorld().getBlockAt(bL.clone().add(0, -1, 0));
        {
            long delay = System.currentTimeMillis() - p.placeTime;
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
                    if (p.isOnGround()) {
                        if (v == placeHash || bL.distance(l) < 1.2) {
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
                    } else if (bL.distance(l) < 2.35) {
                        checkHash = true;
                        break;
                    }
                }
            }
            if (delay < 5
                    || placeHash == p.placeHash
                    || (long) p.getLocation().getY() <= bL.getY()
                    || p.bukkit.isFlying() || !checkHash) {
                p.rightClickCounter = 0;
                p.placeTime = System.currentTimeMillis();
                p.oldBlockEvent = e;
                /*
                p.player.sendMessage("c 1 " + (delay < 5)
                                + " " + (placeHash == p.placeHash) + " "
                                + ((long) p.getLocation().getY() <= bL.getY())
                                + " " + p.player.isFlying() + " " + !checkHash);
                 */
                return;
            } else if (bL.getY() != oldB.getLocation().getY()
                    || !downB.getType().name().equals("AIR")) {
                p.rightClickCounter = 0;
                p.oldBlockEvent = e;
                return;
            }

            if (delay < 280) {
                buffer.count += 6;
            } else if (delay < (p.isSneaking() ? 345 : 385) && p.rightClickCounter < 3) {
                buffer.count += 3;
            } else {
                flag = false;
            }

            if (buffer.count > 10 && flag) {
                this.cancel("type: scaffold analysis ("
                        + (long) Math.abs((((double) 400 / (double) delay) * 100) - 100)
                        + "% faster than possible) [VL: " + buffer.count + "]");
                buffer.count -= 3;
            } else if (buffer.count > 0) {
                buffer.count--;
            }
            //p.player.sendMessage(delay + " " + p.rightClickCounter);

        }

        p.oldBlockEvent = e;
        p.placeHash = placeHash;
        p.placeTime = System.currentTimeMillis();
        p.rightClickCounter = 0;
    }

}
