package com.vagdedes.spartan.checks.combat.fastClicks;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class ClicksPerSecond extends DetectionExecutor {

    ClicksPerSecond(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    boolean run() {
        int count = player.getClicks().getCount(),
                limit = hackType.getCheck().getNumericalOption(
                        "clicks_per_second_limit",
                        FastClicks.defaultClicksPerSecondLimit
                );

        if (count >= limit) {
            cancel(
                    "type: clicks-per-second"
                            + ", count: " + count
                            + ", limit: " + limit
            );
            return true;
        } else {
            return false;
        }
    }
}
