package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import me.vagdedes.spartan.system.Enums.HackType;

public class Speed extends CheckExecutor {

    private final SpeedMotion motion;
    private final SpeedFoodSprint foodSprint;

    public Speed(SpartanPlayer player) {
        super(HackType.Speed, player, 2);
        this.addDetections(new DetectionExecutor[]{
                motion = new SpeedMotion(this),
                foodSprint = new SpeedFoodSprint(this)
        });
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {

    }

    @Override
    public void runInternal(boolean cancelled) {
        if (canDo()) {
            motion.run();
            foodSprint.run();
        }
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
