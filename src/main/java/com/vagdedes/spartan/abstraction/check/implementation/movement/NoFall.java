package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class NoFall extends CheckExecutor {

    private double[] previous;

    public NoFall(SpartanPlayer player) {
        super(HackType.NoFall, player);
        this.previous = new double[]{0.0, 0.0};
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {

    }

    @Override
    public void runInternal(boolean cancelled) {
        if (!canDo()) {
            this.previous = new double[]{0.0, 0.0};
            return;
        }
        Double vertical = player.movement.getNmsVerticalDistance();

        if (vertical == null) {
            this.previous = new double[]{0.0, 0.0};
            return;
        }
        float fallDistance = player.getFallDistance();
        int fallTick = Math.max(
                player.movement.getFallTick(
                        vertical,
                        -PlayerUtils.airAcceleration,
                        PlayerUtils.airDrag,
                        GroundUtils.maxHeightLengthRatio
                ) - 1,
                0
        );
        List<Double> jumpMotions = PlayerUtils.getJumpMotions(
                PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1
        );
        double drag = 0.98,
                jumpDifference = jumpMotions.get(jumpMotions.size() - 1),
                acceleration1 = PlayerUtils.airAcceleration * drag,
                acceleration2 = acceleration1 - jumpDifference,
                prediction1 = fallTick == 0 ? 0.0 :
                        fallTick == 1 ? acceleration1 :
                                (this.previous[0] * drag) + (fallTick * acceleration1),
                prediction2 = fallTick == 0 ? 0.0 :
                        fallTick == 1 ? acceleration2
                                : (this.previous[1] * drag) + (fallTick * acceleration1) - jumpDifference;
        this.previous[0] = prediction1;
        this.previous[1] = prediction2;
        player.debug(false, false, true,
                fallTick,
                fallDistance,
                prediction1,
                prediction2,
                player.isOnGround()
        );
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
