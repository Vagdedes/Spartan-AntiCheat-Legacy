package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import org.bukkit.GameMode;

public class IrregularMovementsFoodSprint extends DetectionExecutor {

    IrregularMovementsFoodSprint(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void run() {
        if (hackType.getCheck().getBooleanOption("check_sprint", true)
                && player.movement.isSprinting()) {
            GameMode gameMode = player.getGameMode();
            int food = player.getFoodLevel();

            if (food <= 6
                    && (gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE)) {
                cancel(
                        "type: food-sprint"
                                + ", game-mode: " + gameMode
                                + ", food: " + food,
                        1.0,
                        player.movement.getDetectionLocation(),
                        0,
                        true
                );
            }
        }
    }
}
