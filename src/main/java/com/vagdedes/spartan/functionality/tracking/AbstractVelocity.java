package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class AbstractVelocity {

    static final int blocksOffGround = AlgebraUtils.integerRound(PlayerUtils.height / 4.0);

    public static boolean hasCooldown(SpartanPlayer p) {
        return getRemainingTicks(p) > 0;
    }

    public static int getRemainingTicks(SpartanPlayer p) {
        int remainingTicks = p.getHandlers().getRemainingTicks(Handlers.HandlerType.AbstractVelocity);

        if (remainingTicks > 0) {
            int blockY = p.movement.getLocation().getBlockY();

            if (blockY <= 0 || p.movement.getTicksOnAir() <= 20) {
                return remainingTicks;
            }
            int offGroundBlocks = p.getBlocksOffGround(blocksOffGround + 1); // Do not use limit because of the 'blockY' comparison

            if (offGroundBlocks <= blocksOffGround || offGroundBlocks == blockY) {
                return remainingTicks;
            }
        }
        return 0;
    }
}
