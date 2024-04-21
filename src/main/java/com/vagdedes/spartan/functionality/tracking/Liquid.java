package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

public class Liquid {

    public static boolean runMove(SpartanPlayer p) {
        if (p.movement.isSwimming() || isLocation(p, p.movement.getLocation())) {
            p.movement.setLastLiquidTime();

            if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    && Compatibility.CompatibilityType.ViaVersion.isFunctional()
                    && Math.round(p.getEyeHeight() * 10.0) == 4.0) {
                p.movement.setSwimming(false, 10);
            }
            WaterElevator.runMove(p);
            return true;
        }
        return false;
    }

    public static boolean isLocation(SpartanPlayer player, SpartanLocation location) {
        double hitbox = player.bedrockPlayer ? BlockUtils.hitbox_max : BlockUtils.hitbox;
        return location.getBlock().isLiquidOrWaterLogged()
                || PatternUtils.isLiquidPattern(new double[][]{
                {hitbox, 0, hitbox},
                {hitbox, 1, hitbox},
                {hitbox, player.getEyeHeight() + PlayerUtils.lowPrecision, hitbox}
        }, location, true);
    }
}
