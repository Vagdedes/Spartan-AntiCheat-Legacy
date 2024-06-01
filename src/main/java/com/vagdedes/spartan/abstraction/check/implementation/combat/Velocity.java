package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class Velocity extends CheckExecutor {

    public Velocity(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof EntityDamageByEntityEvent) {
            // todo
        } else if (object instanceof PlayerVelocityEvent) {
            // todo
        }
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player)
                && !player.isOutsideOfTheBorder(-1.0);
    }
}
