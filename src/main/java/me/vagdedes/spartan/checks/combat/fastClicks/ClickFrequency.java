package me.vagdedes.spartan.checks.combat.fastClicks;

// Study of the time between clicks

import me.vagdedes.spartan.handlers.tracking.ClickData;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class ClickFrequency {

    private static final String
            key = ClickFrequency.class.getName(),
            detection = "click-frequency";
    private static final int
            bufferThreshold = 5,
            bufferFailure = 3,
            bufferTime = 5 * 20;

    static void run(SpartanPlayer player, ClickData clickData, int latencyExtra, int extraLimit, double globalAverage) {
        int count = clickData.getCount(), limit = FastClicks.suspiciousClicksPerSecond + extraLimit;

        if (globalAverage > limit) {
            limit = AlgebraUtils.integerCeil(globalAverage) + 1;
        }
        limit += latencyExtra;

        if (count >= limit) {
            long passedTimeDifference = clickData.getPassedTimeDifference();

            if (passedTimeDifference <= FastClicks.suspiciousMillisecondCorrelation) {
                Buffer buffer = player.getBuffer(FastClicks.check);

                if (buffer.start(key, bufferTime) == bufferThreshold) {
                    buffer.remove(key);
                    FastClicks.violate(player,
                            null,
                            "type: " + detection
                                    + ", count: " + count
                                    + ", limit: " + limit
                                    + ", latency-extra: " + latencyExtra
                                    + ", global-average: " + globalAverage
                                    + ", average-pattern: " + clickData.getAveragePattern()
                                    + ", last-pattern: " + clickData.getLastPattern()
                                    + ", pattern-difference: " + clickData.getPatternDifference()
                                    + ", average-passed-time: " + clickData.getAveragePassedTime()
                                    + ", last-passed-time: " + clickData.getLastPassedTime()
                                    + ", passed-time-difference: " + passedTimeDifference
                    );
                }
            } else {
                player.getBuffer(FastClicks.check).decrease(key, bufferFailure);
            }
        }
    }
}
