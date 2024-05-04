package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import me.vagdedes.spartan.system.Enums.HackType;

public class Speed extends CheckExecutor {

    public static final HackType check = HackType.Speed;
    private final SpeedMotion motion;
    private final SpeedSprint sprint;

    public Speed(SpartanPlayer player) {
        super(check, player, 2);
        this.addDetections(new DetectionExecutor[]{
                motion = new SpeedMotion(this),
                sprint = new SpeedSprint(this)
        });
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            // todo
        }
        return false;
    }

    @Override
    public void scheduler() {

    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && MovementProcessing.canCheck(player, false, true, false);
    }
}
