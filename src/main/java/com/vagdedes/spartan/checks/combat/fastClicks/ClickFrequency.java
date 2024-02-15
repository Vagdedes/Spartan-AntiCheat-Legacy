package com.vagdedes.spartan.checks.combat.fastClicks;

// Study of the time between clicks

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.data.Clicks;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class ClickFrequency extends DetectionExecutor {

    private final FastClicks fastClicks;

    ClickFrequency(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        fastClicks = (FastClicks) executor;
    }

    private static final String
            key = ClickFrequency.class.getName(),
            detection = "click-frequency";
    private static final int
            bufferThreshold = 5,
            bufferFailure = 3,
            bufferTime = 5 * 20;

    void run(Clicks clickData, int extraLimit, double globalAverage) {
        int count = clickData.getCount(), limit = FastClicks.suspiciousClicksPerSecond + extraLimit;

        if (globalAverage > limit) {
            limit = AlgebraUtils.integerCeil(globalAverage) + 1;
        }

        if (count >= limit) {
            long passedTimeDifference = clickData.getPassedTimeDifference();

            if (passedTimeDifference <= FastClicks.suspiciousMillisecondCorrelation) {
                Buffer buffer = player.getBuffer(hackType);

                if (buffer.start(key, bufferTime) == bufferThreshold) {
                    buffer.remove(key);
                    fastClicks.violate(
                            null,
                            "type: " + detection
                                    + ", count: " + count
                                    + ", limit: " + limit
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
                player.getBuffer(hackType).decrease(key, bufferFailure);
            }
        }
    }
}
