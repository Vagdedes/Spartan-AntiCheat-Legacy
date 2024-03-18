package com.vagdedes.spartan.checks.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;

public class IrregularMovementsNew extends CheckExecutor {

    public static final HackType check = HackType.IrregularMovements;

    public IrregularMovementsNew(SpartanPlayer player) {
        super(HackType.IrregularMovements, player);
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return false;
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        Object[] objects = (Object[]) object;

        for (int count : new int[]{1}) {
            String detection = linearPredictionToDetection(
                    SpartanBukkit.movementPatterns.vertical,
                    (long) objects[0],
                    GroundUtils.maxHeightLength,
                    count,
                    (Number) objects[1]
            );

            if (detection != null) {
                player.debug(false, false, detection);
                player.debug(false, false, "-");
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true);
    }
}