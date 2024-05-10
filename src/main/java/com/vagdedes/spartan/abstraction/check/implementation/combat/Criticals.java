package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

public class Criticals extends CheckExecutor {

    public Criticals(SpartanPlayer player) {
        super(HackType.Criticals, player);
    }

    @Override
    protected boolean canDo() {
        return CombatProcessing.canCheck(player)
                && !player.movement.wasInLiquids()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS, 0);
    }

    @Override
    public void runInternal(boolean cancelled) {
        if (canDo()) {
            // todo
        }
    }

    @Override
    public void schedulerInternal() {

    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {

    }
}
