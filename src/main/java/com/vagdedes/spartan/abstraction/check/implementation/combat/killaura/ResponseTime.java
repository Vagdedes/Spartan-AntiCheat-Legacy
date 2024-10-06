package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class ResponseTime extends DetectionExecutor {

    ResponseTime(CheckExecutor executor) {
        super(executor, "response_time", true);
    }

    void run() {
        // todo
    }
}
