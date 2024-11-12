package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetectionExecutor;

public class HitDirection extends ProbabilityDetectionExecutor {

    HitDirection(CheckRunner executor) {
        super(executor, "hit_direction", true);
    }

    void run() {
        // todo
    }

}
