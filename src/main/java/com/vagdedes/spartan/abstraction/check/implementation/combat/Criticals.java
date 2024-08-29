package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

public class Criticals extends CheckExecutor {

    public Criticals(HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, false);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        double verticalMaxAccuracy = player.movement.getEventVertical();

        if (RayUtils.onBlock(player, player.movement.getLocation(), BlockUtils.web)) return;

        if ((verticalMaxAccuracy < 0.079 || Math.abs(verticalMaxAccuracy) == 0.0625)
                && verticalMaxAccuracy > 0
                && player.movement.getTicksOnAir() == 1
                        && !player.movement.getLocation().getBlock().material.isBlock()
                && !RayUtils.onSolidBlock(player, player.movement.getLocation().clone().add(0, 2, 0))) {
            cancel("type: packet", 1);
        }
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player)
                && !player.movement.isInLiquids()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS, 0);
    }
}
