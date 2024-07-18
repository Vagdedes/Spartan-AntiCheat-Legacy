package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion;

import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class BlockUtils {
    public static float getSlipperiness(Block block) {
        SpartanBlock spartanBlock = new SpartanBlock(block);
        Material material = block.getType();
        if (material.equals(Material.SLIME_BLOCK)) {
            return 0.8F;
        }
        if (material.toString().contains("ICE")) {
            return 0.98F;
        }
        if (material.toString().equals("BLUE_ICE")) {
            return 0.989F;
        }
        return 0.6F;
    }
}
