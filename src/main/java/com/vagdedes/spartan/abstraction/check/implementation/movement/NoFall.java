package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;

public class NoFall extends CheckExecutor {

    private static final double precision = 0.001;

    private float previousFall;

    public NoFall(SpartanPlayer player) {
        super(HackType.NoFall, player);
        this.previousFall = Float.MIN_VALUE;
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {

    }

    @Override
    public void runInternal(boolean cancelled) {
        if (canDo() && false) {
            float fallDistance = player.getFallDistance();

            if (previousFall != Float.MIN_VALUE) {
                int airTick = getAirTick(fallDistance),
                        previousAirTick = getAirTick(previousFall);
                player.debug(true, false, false,
                        fallDistance,
                        player.movement.getTicksOnAir(),
                        airTick - previousAirTick,
                        (previousFall * PlayerUtils.airDrag) + (PlayerUtils.airAcceleration * (airTick - previousAirTick))
                );
            }
            previousFall = fallDistance;
        } else {
            previousFall = Float.MIN_VALUE;
        }
    }

    private int getAirTick(float fallDistance) {
        return Math.max(player.movement.getFallTick(-fallDistance, precision) - 1, 0);
    }

    @Override
    public void scheduler() {

    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && MovementProcessing.canCheck(player, false, false, false)
                && player.getVehicle() == null
                && !player.movement.wasInLiquids()
                && !BlockUtils.canClimb(player.movement.getLocation().getBlock().material);
    }
}
