package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion;

import com.vagdedes.spartan.abstraction.world.SpartanBlock;

public class BlockUtils {

    public static float getSlipperiness(SpartanBlock block) {
        if (com.vagdedes.spartan.utils.minecraft.world.BlockUtils.areSlimeBlocks(block.material)) {
            return 0.8F;
        } else if (com.vagdedes.spartan.utils.minecraft.world.BlockUtils.areRegularIceBlocks(block.material)) {
            return 0.98F;
        } else if (com.vagdedes.spartan.utils.minecraft.world.BlockUtils.areBlueIceBlocks(block.material)) {
            return 0.989F;
        } else {
            return 0.6F;
        }
    }
}
