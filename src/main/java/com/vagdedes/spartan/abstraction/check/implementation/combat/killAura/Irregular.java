package com.vagdedes.spartan.abstraction.check.implementation.combat.killAura;

// Study of hits that couldn't have been dealt

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;

public class Irregular extends DetectionExecutor {

    Irregular(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (player.isDead()) {
            cancel("type: irregular(death)");
        } else if (player.isSleeping()) {
            cancel("type: irregular(sleeping)");
        } else if (player.getOpenInventory().isUsingTheCursor()) {
            cancel("type: irregular(cursor)");
        } else if (player.getOpenInventory().isUsingAnotherInventory()) {
            cancel("type: irregular(inventory)");
        }
    }
}
