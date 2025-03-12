package com.vagdedes.vacan.compatibility.manual.building;

import com.vagdedes.vacan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.vacan.compatibility.Compatibility;
import com.vagdedes.vacan.utils.minecraft.entity.PlayerUtils;

public class SuperPickaxe {

    public static boolean isUsing(PlayerProtocol p) {
        return Compatibility.CompatibilityType.SUPER_PICKAXE.isFunctional()
                && PlayerUtils.isPickaxeItem(p.getItemInHand().getType());
    }

}
