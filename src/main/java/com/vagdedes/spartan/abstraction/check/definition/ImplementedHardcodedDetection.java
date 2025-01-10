package com.vagdedes.spartan.abstraction.check.definition;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;

public class ImplementedHardcodedDetection extends HardcodedDetection {

    public ImplementedHardcodedDetection(CheckRunner executor, Check.DataType forcedDataType, String name, boolean def) {
        super(executor, forcedDataType, name, def);
    }

    public ImplementedHardcodedDetection(CheckDetection executor, String name, boolean def) {
        super(executor.executor, executor.forcedDataType, name, def);
    }

}
