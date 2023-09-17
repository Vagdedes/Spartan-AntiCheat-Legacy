package me.vagdedes.spartan.checks.combat.killAura;

// Study of hits that couldn't have been dealt

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
import me.vagdedes.spartan.utils.gameplay.PlayerData;

public class Irregular extends DetectionExecutor {

    Irregular(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (player.isDead()) {
            cancel("type: irregular(death)");
        } else if (player.isSleeping()) {
            cancel("type: irregular(sleeping)");
        } else if (PlayerData.isUsingTheCursor(player)) {
            cancel("type: irregular(cursor)");
        } else if (PlayerData.isUsingAnInventory(player, 8)) {
            cancel("type: irregular(inventory)");
        }
    }
}
