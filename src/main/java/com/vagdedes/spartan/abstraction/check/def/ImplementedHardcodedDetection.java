package com.vagdedes.spartan.abstraction.check.def;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.HardcodedDetectionExecutor;

public class ImplementedHardcodedDetection extends HardcodedDetectionExecutor {

    public ImplementedHardcodedDetection(CheckExecutor executor, String name, boolean def) {
        super(executor, name, def);
    }

    public ImplementedHardcodedDetection(DetectionExecutor executor, String name, boolean def) {
        super(executor, name, def);
    }

}
