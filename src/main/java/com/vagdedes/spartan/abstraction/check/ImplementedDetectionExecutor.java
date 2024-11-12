package com.vagdedes.spartan.abstraction.check;

public class ImplementedDetectionExecutor extends ProbabilityDetectionExecutor {

    public ImplementedDetectionExecutor(CheckExecutor executor, String name, boolean def) {
        super(executor, name, def);
    }

    public ImplementedDetectionExecutor(DetectionExecutor executor, String name, boolean def) {
        super(executor, name, def);
    }

}
