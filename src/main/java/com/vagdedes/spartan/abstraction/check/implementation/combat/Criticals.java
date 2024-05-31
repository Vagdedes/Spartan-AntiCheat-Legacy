package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

public class Criticals extends CheckExecutor {

    public Criticals(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    @Override
    protected boolean canRun() {
        return CombatProcessing.canCheck(player)
                && !player.movement.wasInLiquids()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS, 0);
    }

    @Override
    protected void runInternal(boolean cancelled) {
        // todo
    }
}
