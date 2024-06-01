package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.entity.LivingEntity;

public class HitReach extends CheckExecutor {

    public HitReach(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        PlayerAttackEvent event = (PlayerAttackEvent) object;
        LivingEntity entity = event.getTarget();

        if (CombatProcessing.canCheck(player, entity)) {
            // todo
        }
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player);
    }
}
