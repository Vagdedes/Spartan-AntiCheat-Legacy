package com.vagdedes.spartan.checks.combat.killAura;

// Study of hits that couldn't have been dealt

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

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
        } else if (PlayerUtils.isUsingAnInventory(player, 8)) {
            cancel("type: irregular(inventory)");
        }
    }
}
