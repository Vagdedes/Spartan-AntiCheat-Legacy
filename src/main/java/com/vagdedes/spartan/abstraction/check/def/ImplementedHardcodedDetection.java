package com.vagdedes.spartan.abstraction.check.def;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.HardcodedDetectionExecutor;

public class ImplementedHardcodedDetection extends HardcodedDetectionExecutor {

    public ImplementedHardcodedDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);
    }

    public ImplementedHardcodedDetection(CheckDetection executor, String name, boolean def) {
        super(executor, name, def);
    }

}
