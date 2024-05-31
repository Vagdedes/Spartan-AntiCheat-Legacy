package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class MoveDirectionML extends DetectionExecutor {

    MoveDirectionML(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void runMove() {
        // todo
    }

}
