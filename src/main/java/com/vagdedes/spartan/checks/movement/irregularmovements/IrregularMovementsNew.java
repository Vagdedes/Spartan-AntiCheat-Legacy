package com.vagdedes.spartan.checks.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import com.vagdedes.spartan.utils.math.probability.ProbabilityPredictor;
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

        for (int count : new int[]{MoveUtils.fallingMotions, MoveUtils.jumpingMotions}) {
            long situation = (long) objects[0];
            ProbabilityPredictor predictor = buildPredictor(
                    player,
                    SpartanBukkit.movementPatterns.vertical,
                    situation,
                    GroundUtils.maxHeightLength,
                    count
            );
            double minProbability, minSignificance;

            switch (count) {
                default:
                    minProbability = 0.1;
                    minSignificance = 0.1;
                    break;
            }
            String detection = linearPredictionToDetection(
                    SpartanBukkit.movementPatterns.vertical,
                    (double) objects[2],
                    predictor,
                    minProbability,
                    minSignificance
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