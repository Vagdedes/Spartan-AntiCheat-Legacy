package com.vagdedes.spartan.abstraction.check.def;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetectionExecutor;

public class ImplementedProbabilityDetection extends ProbabilityDetectionExecutor {

    public ImplementedProbabilityDetection(CheckExecutor executor, String name, boolean def) {
        super(executor, name, def);
    }

    public ImplementedProbabilityDetection(DetectionExecutor executor, String name, boolean def) {
        super(executor, name, def);
    }

}
