package com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.functionality.server.TPS;

import java.util.Collection;
import java.util.Iterator;

public class FastClicksBukkit extends ProbabilityDetection {

    private final CheckDetection clicksPattern;

    FastClicksBukkit(CheckRunner executor) {
        super(executor, null, Check.DetectionType.BUKKIT, null, true);
        this.clicksPattern = new ImplementedProbabilityDetection(this, "click_pattern", true);
    }

    void run() {
        this.call(() -> {
            int limit = hackType.getCheck().getNumericalOption(
                    "clicks_per_second_limit",
                    15
            );
            boolean checkLimit = limit > 0,
                    checkPattern = this.clicksPattern.isEnabled();

            if (checkLimit || checkPattern) {
                Collection<Long> data = this.protocol.spartan.clicks.getData();
                int count = data.size();
                checkLimit &= count >= limit;
                checkPattern &= count > 1;

                if (checkLimit || checkPattern) {
                    Iterator<Long> iterator = data.iterator();
                    long difference = -1L,
                            previous = iterator.next();
                    double deviation = 0.0;

                    while (iterator.hasNext()) {
                        long current = iterator.next();
                        difference = current - previous;
                        deviation += difference * difference;
                        previous = current;
                    }
                    deviation = Math.sqrt(deviation / (count - 1.0));

                    if (deviation > 10.0) { // Used for latency to judge when many packets are sent in a short period of time
                        if (checkLimit) {
                            this.cancel(
                                    "type: clicks-per-second"
                                            + ", count: " + count
                                            + ", limit: " + limit
                                            + ", deviation: " + deviation
                            );
                        }
                        if (checkPattern
                                && difference != -1L
                                && count > 10) {
                            double absolute = Math.abs(difference - deviation);

                            if (absolute < TPS.tickTime) {
                                this.clicksPattern.cancel(
                                        "type: clicks-pattern"
                                                + ", count: " + count
                                                + ", difference: " + absolute
                                                + ", deviation: " + deviation
                                );
                            }
                        }
                    }
                }
            }
        });
    }

}
