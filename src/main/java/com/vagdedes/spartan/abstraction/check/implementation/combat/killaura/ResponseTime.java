package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class ResponseTime extends DetectionExecutor {

    ResponseTime(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void runDamage() {
        // todo
    }

    void runMove() {
        // todo
    }
}
