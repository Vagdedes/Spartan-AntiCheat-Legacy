package com.vagdedes.spartan.abstraction.check.implementation.combat.fastclicks;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;

public class FastClicksPackets extends ProbabilityDetection {

    FastClicksPackets(CheckRunner executor) {
        super(executor, "packets", true);
    }

}
