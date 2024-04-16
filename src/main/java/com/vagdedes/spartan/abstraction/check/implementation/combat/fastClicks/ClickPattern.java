package com.vagdedes.spartan.abstraction.check.implementation.combat.fastClicks;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class ClickPattern extends DetectionExecutor {

    ClickPattern(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    boolean run() {
        if (hackType.getCheck().getBooleanOption("check_click_pattern", true)) {

        }
        return false;
    }
}
