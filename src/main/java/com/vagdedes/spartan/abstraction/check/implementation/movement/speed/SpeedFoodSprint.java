package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import org.bukkit.GameMode;

public class SpeedFoodSprint extends DetectionExecutor {

    SpeedFoodSprint(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (hackType.getCheck().getBooleanOption("check_sprint", true)) {
            GameMode gameMode = player.getGameMode();
            boolean sprinting = player.movement.isSprinting();
            int food = player.getFoodLevel();

            runAsync(() -> {
                if (sprinting && food <= 6
                        && (gameMode == GameMode.SURVIVAL
                        || gameMode == GameMode.ADVENTURE)) {
                    player.debug(
                            true, false, false,
                            food
                    );
                }
            });
        }
    }
}
