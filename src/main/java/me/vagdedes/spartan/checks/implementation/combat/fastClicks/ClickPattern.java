package me.vagdedes.spartan.checks.implementation.combat.fastClicks;

// Study of the time between clicks in regard to pattern

import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.handlers.tracking.ClickData;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class ClickPattern {

    private static final String
            key = ClickPattern.class.getName(),
            detection = "click-pattern";
    private static final int
            bufferThreshold = 4,
            bufferFailure = 2,
            bufferTime = 5 * 20;
    private static final double patternPrecision = (1.0 / TPS.tickTimeDecimal) * FastClicks.suspiciousMillisecondCorrelation; // Last number represents the time in milliseconds

    static void run(SpartanPlayer player, ClickData clickData, int latencyExtra, int extraLimit, double globalAverage) {
        int count = clickData.getCount(), limit = FastClicks.suspiciousClicksPerSecond + extraLimit;

        if (globalAverage > limit) {
            limit = AlgebraUtils.integerCeil(globalAverage) + 1;
        }
        limit += latencyExtra;

        if (count >= limit) {
            double patternRequirement = AlgebraUtils.ceilToHalf(ClickData.pastStoredTime / (limit * TPS.tickTimeDecimal));
            double averagePattern = clickData.getAveragePattern();

            if (averagePattern <= patternRequirement) {
                double lastPattern = clickData.getLastPattern();

                if (Math.abs(lastPattern - averagePattern) <= patternPrecision) {
                    Buffer buffer = player.getBuffer(FastClicks.check);

                    if (buffer.start(key, bufferTime) == bufferThreshold) {
                        buffer.remove(key);
                        FastClicks.violate(player,
                                null,
                                "type: " + detection
                                        + ", count: " + count
                                        + ", limit: " + limit
                                        + ", pattern-requirement: " + patternRequirement
                                        + ", pattern-precision: " + patternPrecision
                                        + ", latency-extra: " + latencyExtra
                                        + ", global-average: " + globalAverage
                                        + ", average-pattern: " + averagePattern
                                        + ", last-pattern: " + lastPattern
                                        + ", pattern-difference: " + clickData.getPatternDifference()
                                        + ", average-passed-time: " + clickData.getAveragePassedTime()
                                        + ", last-passed-time: " + clickData.getLastPassedTime()
                                        + ", passed-time-difference: " + clickData.getPassedTimeDifference()
                        );
                    }
                } else {
                    player.getBuffer(FastClicks.check).decrease(key, bufferFailure);
                }
            }
        }
    }
}
