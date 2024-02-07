package com.vagdedes.spartan.checks.combat.fastClicks;

// Study of the time between clicks in regard to pattern

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.data.Clicks;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class ClickPattern extends DetectionExecutor {

    private final FastClicks fastClicks;

    ClickPattern(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        fastClicks = (FastClicks) executor;
    }

    private static final String
            key = ClickPattern.class.getName(),
            detection = "click-pattern";
    private static final int
            bufferThreshold = 4,
            bufferFailure = 2,
            bufferTime = 5 * 20;
    private static final double patternPrecision = (1.0 / TPS.tickTimeDecimal) * FastClicks.suspiciousMillisecondCorrelation; // Last number represents the time in milliseconds

    void run(Clicks clickData, int extraLimit, double globalAverage) {
        int count = clickData.getCount(), limit = FastClicks.suspiciousClicksPerSecond + extraLimit;

        if (globalAverage > limit) {
            limit = AlgebraUtils.integerCeil(globalAverage) + 1;
        }

        if (count >= limit) {
            double patternRequirement = AlgebraUtils.ceilToHalf(Clicks.pastStoredTime / (limit * TPS.tickTimeDecimal));
            double averagePattern = clickData.getAveragePattern();

            if (averagePattern <= patternRequirement) {
                double lastPattern = clickData.getLastPattern();

                if (Math.abs(lastPattern - averagePattern) <= patternPrecision) {
                    Buffer buffer = player.getBuffer(hackType);

                    if (buffer.start(key, bufferTime) == bufferThreshold) {
                        buffer.remove(key);
                        fastClicks.violate(
                                null,
                                "type: " + detection
                                        + ", count: " + count
                                        + ", limit: " + limit
                                        + ", pattern-requirement: " + patternRequirement
                                        + ", pattern-precision: " + patternPrecision
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
                    player.getBuffer(hackType).decrease(key, bufferFailure);
                }
            }
        }
    }
}
