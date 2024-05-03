package com.vagdedes.spartan.abstraction.check.implementation.combat.killAura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class MoveDirection extends DetectionExecutor {

    MoveDirection(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void runMove() {
        // todo
    }

}
