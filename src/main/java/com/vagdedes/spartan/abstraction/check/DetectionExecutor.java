package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.pattern.*;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPredictor;
import me.vagdedes.spartan.system.Enums;

public abstract class DetectionExecutor {

    public final CheckExecutor executor;
    public final Enums.HackType hackType;
    public final SpartanPlayer player;

    public DetectionExecutor(CheckExecutor executor, Enums.HackType hackType, SpartanPlayer player) {
        this.executor = executor;
        this.hackType = hackType;
        this.player = player;
    }

    protected final void runAsync(Runnable runnable) {
        if (!MultiVersion.folia
                && (hackType.getCheck().isSilent(player.getWorld().getName())
                || TPS.getMillisecondsPassed(player) <= 40L)) {
            SpartanBukkit.detectionThread.executeIfFreeElseHere(runnable);
        } else {
            // If there are less than 10 milliseconds available and the check is not silent,
            // we definitely run the detection on the main thread because we run into the
            // danger of moving the possible prevention in the next tick
            runnable.run();
        }
    }

    protected final void forceAsync(Runnable runnable) {
        SpartanBukkit.detectionThread.execute(runnable);
    }

    // Spearator

    protected final void cancel(String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport, double damage, int violation) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, damage, violation);
    }

    protected final void cancel(String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport, double damage) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, damage, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, 0.0, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location, int teleportCooldown) {
        new HackPrevention(player, hackType, verbose, location, teleportCooldown, false, 0.0, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location) {
        new HackPrevention(player, hackType, verbose, location, 0, false, 0.0, 1);
    }

    protected final void cancel(String verbose) {
        new HackPrevention(player, hackType, verbose, null, 0, false, 0.0, 1);
    }

    // Separator

    protected final void linearMLDetection(Pattern patternStorage,
                                           int generalization,
                                           int situation,
                                           double client,
                                           short count) {
        long time = System.currentTimeMillis();

        if (generalization != 0) {
            client = AlgebraUtils.cut(client, generalization);
        }
        LinearPatternGeneralization patternGeneralization =
                (LinearPatternGeneralization) patternStorage.getGeneralization(generalization);
        LinearPatternProfile patternProfile = patternGeneralization.getProfile(player.getProfile());
        ProbabilityPredictor probabilityPredictor = patternGeneralization.getLinearDataPrediction(
                patternProfile,
                situation,
                count
        );
        patternProfile.updateComparison(
                time,
                client,
                probabilityPredictor.pie.getProbability(client, 1.0),
                probabilityPredictor.pie.getRawTotal()
        );
    }

    protected final void nonLinearMLDetection(Pattern patternStorage,
                                              int generalization,
                                              int situation,
                                              double client) {
        long time = System.currentTimeMillis();

        if (generalization != 0) {
            client = AlgebraUtils.cut(client, generalization);
        }
        NonLinearPatternGeneralization patternGeneralization =
                (NonLinearPatternGeneralization) patternStorage.getGeneralization(generalization);
        double[] probabilityAndTotal = patternGeneralization.getProbabilityAndTotal(situation, client);
        patternGeneralization.getProfile(player.getProfile()).updateComparison(
                time,
                client,
                probabilityAndTotal[0],
                probabilityAndTotal[1]
        );
    }

    protected final void nonLinearRankedMLDetection(Pattern patternStorage,
                                                    int generalization,
                                                    int situation,
                                                    double client) {
        long time = System.currentTimeMillis();

        if (generalization != 0) {
            client = AlgebraUtils.cut(client, generalization);
        }
        NonLinearRankedPatternGeneralization patternGeneralization =
                (NonLinearRankedPatternGeneralization) patternStorage.getGeneralization(generalization);
        double[] probabilityAndPositionAndTotal = patternGeneralization.getProbabilityAndPositionAndTotal(situation, client);
        patternGeneralization.getProfile(player.getProfile()).updateComparison(
                time,
                client,
                probabilityAndPositionAndTotal[0] * probabilityAndPositionAndTotal[1],
                probabilityAndPositionAndTotal[2]
        );
    }
}
