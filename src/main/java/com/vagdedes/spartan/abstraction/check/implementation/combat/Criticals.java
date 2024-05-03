package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

public class Criticals extends CheckExecutor {

    static final HackType check = HackType.Criticals;

    public Criticals(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && CombatProcessing.canCheck(player)
                && !player.movement.wasInLiquids()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS, 0);
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            // todo
        }
        return false;
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }
}
