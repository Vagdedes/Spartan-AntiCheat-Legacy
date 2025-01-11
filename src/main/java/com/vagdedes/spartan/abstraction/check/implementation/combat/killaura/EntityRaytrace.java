package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;

public class EntityRaytrace extends ProbabilityDetection {

    EntityRaytrace(CheckRunner executor) {
        super(executor, null, null, "entity_raytrace", true);
    }

    void run() {
        this.call(() -> {
            // todo
        });
    }

}
