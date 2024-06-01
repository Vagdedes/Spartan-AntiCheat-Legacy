package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class BlockRaytrace extends DetectionExecutor {

    BlockRaytrace(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void runDamage() {
        // todo
    }

}
