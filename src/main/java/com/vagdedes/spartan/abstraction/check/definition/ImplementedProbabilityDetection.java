package com.vagdedes.spartan.abstraction.check.definition;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetectionExecutor;

public class ImplementedProbabilityDetection extends ProbabilityDetectionExecutor {

    public ImplementedProbabilityDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
    }

    public ImplementedProbabilityDetection(CheckDetection executor, String name, boolean def) {
        super(executor, name, def);
    }

}
