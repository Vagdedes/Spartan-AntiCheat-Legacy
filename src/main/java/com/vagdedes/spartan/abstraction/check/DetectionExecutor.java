package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.pattern.implementation.base.Pattern;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternGeneralization;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPredictor;
import com.vagdedes.spartan.utils.math.ProbabilityRank;
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

    protected final String linearPredictionToDetection(Pattern patternStorage,
                                                       int generalization,
                                                       int count,
                                                       long situation,
                                                       Number client) {
        long time = System.currentTimeMillis();
        client = AlgebraUtils.generalize(client, generalization);
        PatternGeneralization patternGeneralization = patternStorage.getGeneralization(generalization);
        PatternProfile patternProfile = patternGeneralization.getProfile(player.getProfile());
        ProbabilityPredictor probabilityPredictor = ProbabilityPredictor.build(
                player,
                situation,
                patternGeneralization,
                patternProfile,
                count
        );
        double[] trustFactorRankAndProbability = patternGeneralization.trustFactorRankAndProbability(patternProfile),
                contributionRankAndProbability = patternGeneralization.contributionRankAndProbability(patternProfile);
        StringBuilder sb = new StringBuilder()
                .append("data: ")
                .append(patternStorage.key)
                .append("(")
                .append(generalization)
                .append("x")
                .append(count)
                .append("), client: ")
                .append(client);

        if (trustFactorRankAndProbability != null) {
            sb.append(", probability: ")
                    .append(AlgebraUtils.cut(
                            trustFactorRankAndProbability[0] * 100.0,
                            2
                    ))
                    .append("(")
                    .append(AlgebraUtils.cut(
                            trustFactorRankAndProbability[1] * 100.0,
                            2
                    ))
                    .append(")");
        }
        if (contributionRankAndProbability != null) {
            sb.append(", contribution: ")
                    .append(AlgebraUtils.cut(
                            contributionRankAndProbability[0] * 100.0,
                            2
                    ))
                    .append("(")
                    .append(AlgebraUtils.cut(
                            contributionRankAndProbability[1] * 100.0,
                            2
                    ))
                    .append(")");
        }

        if (probabilityPredictor.pie.hasData()) {
            patternGeneralization.compare(
                    patternProfile,
                    -1L,
                    time
            );
            Iterator<Map.Entry<Number, Integer>> entries = probabilityPredictor.pie.getProbabilitiesRanked().iterator();

            while (entries.hasNext()) {
                Map.Entry<Number, Integer> entry = entries.next();
                double probability = probabilityPredictor.pie.getProbabilityWithCount(entry.getValue());

                if (entry.getKey().doubleValue() == client.doubleValue()) {
                    patternGeneralization.compare(
                            patternProfile,
                            client,
                            probability,
                            time
                    );
                    return sb.toString();
                } else {
                    count++;
                }
            }
            patternGeneralization.compare(
                    patternProfile,
                    client,
                    1.0 / (probabilityPredictor.pie.getTotal() + 1.0),
                    time
            );
        } else {
            patternGeneralization.compare(
                    patternProfile,
                    time,
                    time
            );
        }
        return sb.toString();
    }

    protected final String rankedPredictionToDetection(Pattern patternStorage,
                                                       int generalization,
                                                       long situation,
                                                       Number client) {
        long time = System.currentTimeMillis();
        client = AlgebraUtils.generalize(client, generalization);
        PatternGeneralization patternGeneralization = patternStorage.getGeneralization(generalization);
        PatternProfile patternProfile = patternGeneralization.getProfile(player.getProfile());
        ProbabilityRank probabilityRank = patternGeneralization.getRanked(player, situation);
        double[] trustFactorRankAndProbability = patternGeneralization.trustFactorRankAndProbability(patternProfile),
                contributionRankAndProbability = patternGeneralization.contributionRankAndProbability(patternProfile);
        StringBuilder sb = new StringBuilder()
                .append("data: ")
                .append(patternStorage.key)
                .append("(")
                .append(generalization)
                .append("), client: ")
                .append(client);

        if (trustFactorRankAndProbability != null) {
            sb.append(", probability: ")
                    .append(AlgebraUtils.cut(
                            trustFactorRankAndProbability[0] * 100.0,
                            2
                    ))
                    .append("(")
                    .append(AlgebraUtils.cut(
                            trustFactorRankAndProbability[1] * 100.0,
                            2
                    ))
                    .append(")");
        }
        if (contributionRankAndProbability != null) {
            sb.append(", contribution: ")
                    .append(AlgebraUtils.cut(
                            contributionRankAndProbability[0] * 100.0,
                            2
                    ))
                    .append("(")
                    .append(AlgebraUtils.cut(
                            contributionRankAndProbability[1] * 100.0,
                            2
                    ))
                    .append(")");
        }

        if (probabilityRank.hasData()) {
            patternGeneralization.compare(
                    patternProfile,
                    -1L,
                    time
            );
            patternGeneralization.compare(
                    patternProfile,
                    client,
                    probabilityRank.getProbability(client),
                    time
            );
        } else {
            patternGeneralization.compare(
                    patternProfile,
                    time,
                    time
            );
        }
        return sb.toString();
    }
}
