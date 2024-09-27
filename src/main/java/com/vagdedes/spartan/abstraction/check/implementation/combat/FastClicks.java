package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;

import java.util.Collection;
import java.util.Iterator;

public class FastClicks extends CheckExecutor {

    public FastClicks(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0);
    }

    @Override
    protected void runInternal(boolean cancelled) {
        int limit = hackType.getCheck().getNumericalOption(
                "clicks_per_second_limit",
                15
        );
        boolean checkLimit = limit > 1,
                checkPattern = hackType.getCheck().getBooleanOption(
                        "check_click_pattern",
                        true
                );

        if (checkLimit || checkPattern) {
            Collection<Long> data = player.clicks.getData();
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
                        cancel(
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
                            cancel(
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
                this.hackType.toString(),
                null,
                (int) TPS.maximum
        );
    }

}
