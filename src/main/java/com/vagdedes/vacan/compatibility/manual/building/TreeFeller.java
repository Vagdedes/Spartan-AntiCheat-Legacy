package com.vagdedes.vacan.compatibility.manual.building;

import com.vagdedes.vacan.compatibility.Compatibility;
import com.vagdedes.vacan.utils.minecraft.world.BlockUtils;
import org.bukkit.block.Block;

public class TreeFeller {

    public static boolean canCancel(Block b) {
        return Compatibility.CompatibilityType.TREE_FELLER.isFunctional()
                && BlockUtils.areWoods(b.getType());
    }
}
