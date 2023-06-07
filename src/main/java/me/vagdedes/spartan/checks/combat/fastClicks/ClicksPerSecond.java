package me.vagdedes.spartan.checks.combat.fastClicks;

// Study of the amount of clicks in a given time

import me.vagdedes.spartan.handlers.tracking.ClickData;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class ClicksPerSecond {

    static void run(SpartanPlayer player, ClickData clickData, int limit, int latencyExtra, double globalAverage) {
        int count = clickData.getCount();

        if (count >= limit) {
            FastClicks.violate(player,
                    "clicks-per-second",
                    "type: clicks-per-second"
                            + ", count: " + count
                            + ", limit: " + limit
                            + ", latency-extra: " + latencyExtra
                            + ", global-average: " + globalAverage
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
