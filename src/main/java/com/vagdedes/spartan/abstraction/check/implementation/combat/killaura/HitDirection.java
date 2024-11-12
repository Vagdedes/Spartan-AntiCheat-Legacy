package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetectionExecutor;

public class HitDirection extends ProbabilityDetectionExecutor {

    HitDirection(CheckExecutor executor) {
        super(executor, "hit_direction", true);
    }

    void run() {
        // todo
    }

}
