package com.vagdedes.spartan.abstraction.check.example;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.HardcodedDetectionExecutor;

public class DetectionExecutorHardcodedExample extends HardcodedDetectionExecutor {

    DetectionExecutorHardcodedExample(CheckExecutor executor) {
        super(
                executor,
                "detection_option_name_in_checks_yml",
                true // Enabled By Default Or Not
        );
    }

}
