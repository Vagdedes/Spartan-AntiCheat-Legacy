package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedHardcodedDetection;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.statistics.StatisticsMath;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.World;

import java.util.Collection;

public class XRay extends CheckRunner {

    private static final double threshold = 2.0 / 3.0;

    private static double[] getStatistics(World.Environment environment, MiningHistory.MiningOre ore) {
        Collection<PlayerProfile> profiles = ResearchEngine.getPlayerProfiles();

        if (profiles.size() < PlayerEvidence.factorRequirement) {
            return new double[]{};
        } else {
            int profileCount = 0;
            double mean = 0.0,
                    variance = 0.0;

            for (PlayerProfile profile : profiles) {
                double minesToTimeRatio = profile.getMiningHistory(ore).getMinesToTimeRatio(environment);

                if (minesToTimeRatio > 0.0) {
                    profileCount++;
                    mean += minesToTimeRatio;
                }
            }
            mean /= profileCount;

            for (PlayerProfile profile : profiles) {
                double minesToTimeRatio = profile.getMiningHistory(ore).getMinesToTimeRatio(environment);

                if (minesToTimeRatio > 0.0) {
                    double difference = minesToTimeRatio - mean;
                    variance += difference * difference;
                }
            }
            double stdDev = Math.sqrt(variance / (double) profileCount);

            if (profileCount >= PlayerEvidence.factorRequirement) {
                return new double[]{
                        mean / (double) profileCount,
                        Math.sqrt(stdDev / (double) profileCount)
                };
            } else {
                return new double[]{};
            }
        }
    }

    private final HardcodedDetection[] detections;

    public XRay(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.detections = new HardcodedDetection[World.Environment.values().length];

        for (World.Environment environment : World.Environment.values()) {
            this.detections[environment.ordinal()] = new ImplementedHardcodedDetection(
                    this,
                    environment.name().toLowerCase(),
                    true
            );
        }
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        Object[] objects = (Object[]) object;
        World.Environment environment = World.Environment.valueOf(objects[0].toString());
        double[] statistics = getStatistics(environment, (MiningHistory.MiningOre) objects[2]);

        if (statistics.length > 0) {
            double individual = ((MiningHistory) objects[1]).getMinesToTimeRatio(environment);

            if (individual > 0.0) {
                double probability = StatisticsMath.getCumulativeProbability(
                        (individual - statistics[0]) / statistics[1]
                );
                this.detections[environment.ordinal()].setHackingRatio(probability);

                if (probability >= threshold) {
                    this.detections[environment.ordinal()].cancel(
                            "Unfair gathering of " + objects[3]
                                    + " in " + environment.name().toLowerCase() + " world"
                                    + ", surpassed " + AlgebraUtils.cut(probability * 100.0, 2)
                                    + "% of players"
                    );
                }
            } else {
                this.detections[environment.ordinal()].setHackingRatio(PlayerEvidence.emptyProbability);
            }
        } else {
            this.detections[environment.ordinal()].setHackingRatio(PlayerEvidence.emptyProbability);
        }
    }

}
