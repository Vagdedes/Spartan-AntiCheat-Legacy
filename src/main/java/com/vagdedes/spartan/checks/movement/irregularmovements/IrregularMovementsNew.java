package com.vagdedes.spartan.checks.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;
import com.vagdedes.spartan.utils.java.HashHelper;
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
        long hash = HashHelper.hashPlayer(player);
        hash = HashHelper.extendLong(hash, HashHelper.hashEnvironment(player, (SpartanLocation) objects[0]));

        for (int count : new int[]{2}) {
            ProbabilityPredictor predictor = buildPredictor(
                    player,
                    SpartanBukkit.movementPatterns.vertical,
                    hash,
                    GroundUtils.maxHeightLength,
                    count
            );
            double minProbability, maxDeviation, minSignificance;

            switch (count) {
                default:
                    minProbability = 0.1;
                    maxDeviation = MoveUtils.gravityAcceleration + MoveUtils.highPrecision;
                    minSignificance = 0.1;
                    break;
            }
            String detection = linearPredictionToDetection(
                    SpartanBukkit.movementPatterns.vertical,
                    (double) objects[1],
                    predictor,
                    minProbability,
                    maxDeviation,
                    minSignificance
            );

            if (detection != null) {
                player.debug(false, false, detection);
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