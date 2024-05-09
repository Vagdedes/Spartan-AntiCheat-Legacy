package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

public class Irregular extends DetectionExecutor {

    Irregular(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (player.isDead()) {
            cancel("type: irregular, scenario: death");
        } else if (player.isSleeping()) {
            cancel("type: irregular, scenario: sleeping");
        } else if (player.getOpenInventory().getCursor() != null) {
            cancel("type: irregular, scenario: cursor");
        } else if (player.getOpenInventory().countSlots() > PlayerUtils.playerInventorySlots) {
            cancel("type: irregular, scenario: inventory");
        }
    }
}
