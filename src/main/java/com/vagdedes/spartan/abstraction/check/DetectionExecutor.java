package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.math.implementation.ProbabilityRank;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternProfile;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternStorage;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.probability.ProbabilityPredictor;
import me.vagdedes.spartan.system.Enums;

import java.util.Iterator;
import java.util.Map;

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

    protected final String linearPredictionToDetection(PatternStorage patternStorage,
                                                       long situation,
                                                       int generalization,
                                                       int count,
                                                       Number client) {
        ProbabilityPredictor probabilityPredictor = ProbabilityPredictor.build(
                player,
                patternStorage,
                situation,
                generalization,
                count
        );
        if (probabilityPredictor.pie != null) {
            if (client instanceof Double) {
                client = AlgebraUtils.cut(client.doubleValue(), generalization);
            }
            PatternProfile patternProfile = patternStorage.getProfile(player.getProfile());
            StringBuilder sb = new StringBuilder()
                    .append("data: ")
                    .append(patternStorage.key)
                    .append("(")
                    .append(generalization)
                    .append("x")
                    .append(count)
                    .append("), client: ")
                    .append(client)
                    .append(", probability: ")
                    .append(AlgebraUtils.cut(
                            patternProfile.probability(generalization) * 100.0,
                            2
                    ))
                    .append(", significance: ")
                    .append(AlgebraUtils.cut(
                            patternProfile.significance(generalization) * 100.0,
                            2
                    ))
                    .append(", contribution: ")
                    .append(AlgebraUtils.cut(
                            patternProfile.contribution(generalization) * 100.0,
                            2
                    ));

            if (probabilityPredictor.pie.hasData()) {
                double significance = patternStorage.getSignificance(client, generalization);
                Iterator<Map.Entry<Number, Integer>> entries = probabilityPredictor.pie.getProbabilitiesRanked().iterator();

                while (entries.hasNext()) {
                    Map.Entry<Number, Integer> entry = entries.next();
                    double probability = probabilityPredictor.pie.getProbabilityWithCount(entry.getValue());

                    if (entry.getKey().doubleValue() == client.doubleValue()) {
                        patternStorage.collect(
                                player.getProfile(),
                                generalization,
                                significance,
                                probability
                        );
                        return sb.toString();
                    } else {
                        count++;
                    }
                }
                patternStorage.collect(
                        player.getProfile(),
                        generalization,
                        significance,
                        0.0
                );
            } else {
                patternStorage.collectNewData(
                        player.getProfile(),
                        generalization
                );
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    protected final String rankedPredictionToDetection(PatternStorage patternStorage,
                                                       int generalization,
                                                       Number client,
                                                       ProbabilityRank probabilityRank,
                                                       double minProbability) {
        if (probabilityRank.hasData()) {
            double probability = probabilityRank.getProbability(client, -1.0);

            if (probability != -1.0
                    && probability < minProbability) {
                return "data: " + patternStorage.key + "(" + generalization + ")"
                        + ", client: " + client
                        + ", total: " + probabilityRank.getTotal()
                        + ", probability: " + probability
                        + ", min-probability: " + minProbability;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
