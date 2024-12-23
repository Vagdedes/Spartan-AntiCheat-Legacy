package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.Collection;
import java.util.Iterator;

public class FastClicks extends CheckRunner {

    private final CheckDetection
            clicksPerSecond,
            clicksPattern;

    public FastClicks(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.clicksPerSecond = new ImplementedProbabilityDetection(this, null, true);
        this.clicksPattern = new ImplementedProbabilityDetection(this, "click_pattern", true);
    }

    @Override
    protected void runInternal(boolean cancelled) {
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
                        this.clicksPerSecond.cancel(
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
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        this.addDisableCause(
                null,
                null,
                AlgebraUtils.integerCeil(TPS.maximum)
        );
    }

}
