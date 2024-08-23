package com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

import java.util.Collection;
import java.util.Iterator;

public class ClicksPerSecond extends DetectionExecutor {

    // Used for latency to judge when many packets are sent in a short period of time
    private static final double ignoreDeviation = 10.0;

    ClicksPerSecond(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        Collection<Long> data = player.clicks.getData();
        int count = data.size(),
                limit = hackType.getCheck().getNumericalOption(
                        "clicks_per_second_limit",
                        15
                );

        if (count >= limit) {
            if (count > 1) {
                Iterator<Long> iterator = data.iterator();
                long previous = iterator.next();
                double deviation = 0.0;

                while (iterator.hasNext()) {
                    long current = iterator.next(),
                            difference = current - previous;
                    deviation += difference * difference;
                    previous = current;
                }
                deviation = Math.sqrt(deviation / (count - 1.0));

                if (deviation <= ignoreDeviation) {
                    return;
                }
            }
            cancel(
                    "type: clicks-per-second"
                            + ", count: " + count
                            + ", limit: " + limit
            );
        }
    }
}
