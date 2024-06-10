package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.math.RayUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

public class Criticals extends CheckExecutor {

    public Criticals(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        SpartanLocation to = player.movement.getEventToLocation();
        SpartanLocation from = player.movement.getEventFromLocation();
        double verticalMaxAccuracy = to.getY() - from.getY();
        if (verticalMaxAccuracy == 0.0625) {
            cancel("type: packet (post)", 1);
        }
        if (verticalMaxAccuracy < 0.079
                        && verticalMaxAccuracy > 0
                        && player.movement.getTicksOnAir() == 1
                        && !RayUtils.onSolidBlock(player, to.clone().add(0, 2, 0))) {
            cancel("type: packet (low)", 1);
        }
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player)
                && !player.movement.wasInLiquids()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS, 0);
    }
}
