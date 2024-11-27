package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;

public class ResponseTime extends ProbabilityDetection {

    ResponseTime(CheckRunner executor) {
        super(executor, "response_time", true);
    }

    final void run() {
        // todo
    }
}
