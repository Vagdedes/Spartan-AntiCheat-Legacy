package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

public class NoFall extends CheckExecutor {

    public NoFall(SpartanPlayer player) {
        super(HackType.NoFall, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {

    }

    @Override
    public void runInternal(boolean cancelled) {
        if (canDo()) {
            Double vertical = player.movement.getNmsVerticalDistance();

            if (vertical == null) {
                return;
            }
            float fallDistance = player.getFallDistance();
            player.debug(true, false, false,
                    fallDistance,
                    player.movement.getFallTick(
                            vertical,
                            PlayerUtils.airAcceleration,
                            PlayerUtils.airDrag,
                            GroundUtils.maxHeightLengthRatio
                    )
            );
        }
    }

    @Override
    public void schedulerInternal() {

    }

    @Override
    protected boolean canDo() {
        return MovementProcessing.canCheck(player, false, false, false)
                && player.getVehicle() == null
                && !player.movement.wasInLiquids()
                && !BlockUtils.canClimb(player.movement.getLocation().getBlock().material)
                && (!PlayerUtils.levitation || !player.hasPotionEffect(PotionEffectType.LEVITATION, 0));
    }
}
