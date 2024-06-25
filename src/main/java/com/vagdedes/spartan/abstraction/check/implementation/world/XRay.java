package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.World;
import org.bukkit.WorldType;

public class XRay extends CheckExecutor {

    public XRay(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        Object[] objects = (Object[]) object;
        MiningHistory miningHistory = (MiningHistory) objects[1];
        int mines = miningHistory.getMines(World.Environment.valueOf(objects[0].toString()));

        if (mines > 0) {
            double globalAverage = ResearchEngine.getMiningHistoryAverage((MiningHistory.MiningOre) objects[2]),
                    average = mines / ((double) miningHistory.getDays());

            if (globalAverage < 0.0) {
                globalAverage = Math.abs(globalAverage);
            } else {
                globalAverage = globalAverage * 1.5;
            }
            double points = AlgebraUtils.cut(average - globalAverage, 2);

            if (points > 0.0) {
                cancel(
                        "Unfair gathering of " + objects[3] + ", surpassed others by " + points + " " + (points == 1.0 ? "point" : "points")
                );
            }
        }
    }

    @Override
    protected boolean canRun() {
        return player.getWorld().getWorldType() != WorldType.FLAT;
    }

}
