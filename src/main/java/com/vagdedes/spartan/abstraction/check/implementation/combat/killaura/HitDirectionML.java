package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class HitDirectionML extends DetectionExecutor {

    HitDirectionML(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        // todo
    }

}
