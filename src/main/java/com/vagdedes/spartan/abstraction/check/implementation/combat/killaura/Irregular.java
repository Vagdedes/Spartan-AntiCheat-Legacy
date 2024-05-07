package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class Irregular extends DetectionExecutor {

    Irregular(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (player.isDead()) {
            cancel("type: irregular, scenario: death");
        } else if (player.isSleeping()) {
            cancel("type: irregular, scenario: sleeping");
        } else if (player.getOpenInventory().isUsingTheCursor()) {
            cancel("type: irregular, scenario: cursor");
        } else if (player.getOpenInventory().isUsingAnotherInventory()) {
            cancel("type: irregular, scenario: inventory");
        }
    }
}
