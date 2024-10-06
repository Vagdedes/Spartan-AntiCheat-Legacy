package com.vagdedes.spartan.abstraction.check.implementation.world.scaffold;

import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ScaffoldAnalysis {
    public static void run(DetectionExecutor d,
                           BlockPlaceEvent e,
                           Map<Integer, Buffer.IndividualBuffer> bufferMap) {
        SpartanProtocol p = d.player.protocol;
        if (p.oldBlockEvent == null) {
            p.oldBlockEvent = e;
            return;
        }
        Location l = p.getLocation();
        Location lB = new Location(l.getWorld(), (long) l.getX(), (long) l.getY(), (long) l.getZ());
        long placeHash = (long) ((lB.getX() * 31) + lB.getZ());
        Buffer.IndividualBuffer buffer = bufferMap.computeIfAbsent(
                        101, k -> new Buffer.IndividualBuffer());
        Block b = e.getBlock();
        Location bL = b.getLocation();
        Block oldB = p.oldBlockEvent.getBlock();
        Block downB = p.player.getWorld().getBlockAt(bL.clone().add(0, -1, 0));
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
                    if (v == placeHash) {
                        checkHash = true;
                        break;
                    }
                }
            }
            if (delay < 5
                || placeHash == p.placeHash
                || (long) p.getLocation().getY() <= bL.getY()
                || p.player.isFlying() || !checkHash) {
                p.rightClickCounter = 0;
                p.placeTime = System.currentTimeMillis();
                p.oldBlockEvent = e;
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
                d.cancel("type: scaffold analysis ("
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
