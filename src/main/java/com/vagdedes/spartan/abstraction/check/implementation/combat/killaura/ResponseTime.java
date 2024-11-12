package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetectionExecutor;

public class ResponseTime extends ProbabilityDetectionExecutor {

    ResponseTime(CheckRunner executor) {
        super(executor, "response_time", true);
    }

    void run() {
        // todo
    }
}
