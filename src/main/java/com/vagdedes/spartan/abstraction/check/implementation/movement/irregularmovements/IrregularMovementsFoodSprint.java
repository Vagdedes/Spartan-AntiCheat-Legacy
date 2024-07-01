package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import org.bukkit.GameMode;

public class IrregularMovementsFoodSprint extends DetectionExecutor {

    private final Buffer.IndividualBuffer buffer;

    IrregularMovementsFoodSprint(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        this.buffer = new Buffer.IndividualBuffer(executor.player);
    }

    void run() {
        if (hackType.getCheck().getBooleanOption("check_sprint", true)
                && player.movement.isSprinting()) {
            GameMode gameMode = player.getInstance().getGameMode();
            int food = player.getInstance().getFoodLevel();

            if (food <= 6
                    && (gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE)
                    && buffer.count(1, 10) == 4) {
                buffer.reset();
                cancel(
                        "type: food-sprint"
                                + ", game-mode: " + gameMode.toString().toLowerCase().replace("_", "-")
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
