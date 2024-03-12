package com.vagdedes.spartan.checks.combat.fastClicks;

// Study of the amount of clicks in a given time

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Clicks;

public class ClicksPerSecond extends DetectionExecutor {

    private final FastClicks fastClicks;

    ClicksPerSecond(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        fastClicks = (FastClicks) executor;
    }

    void run(Clicks clickData, int limit) {
        int count = clickData.getCount();

        if (count >= limit) {
            fastClicks.violate(
                    "clicks-per-second",
                    "type: clicks-per-second"
                            + ", count: " + count
                            + ", limit: " + limit
                            + ", average-pattern: " + clickData.getAveragePattern()
                            + ", last-pattern: " + clickData.getLastPattern()
                            + ", pattern-difference: " + clickData.getPatternDifference()
                            + ", average-passed-time: " + clickData.getAveragePassedTime()
                            + ", last-passed-time: " + clickData.getLastPassedTime()
                            + ", passed-time-difference: " + clickData.getPassedTimeDifference()
            );
        }
    }
}
