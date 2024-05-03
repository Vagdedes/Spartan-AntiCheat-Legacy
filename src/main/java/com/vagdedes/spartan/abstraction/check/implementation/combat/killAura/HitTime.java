package com.vagdedes.spartan.abstraction.check.implementation.combat.killAura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class HitTime extends DetectionExecutor {

    HitTime(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        // todo
    }
}
