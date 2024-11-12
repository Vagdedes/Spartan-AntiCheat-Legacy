package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;

public class HitDirection extends ProbabilityDetection {

    HitDirection(CheckRunner executor) {
        super(executor, "hit_direction", true);
    }

    void run() {
        // todo
    }

}
