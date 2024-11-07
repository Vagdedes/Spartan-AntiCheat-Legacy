package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import org.bukkit.GameMode;

public class IrregularMovementsFoodSprint extends DetectionExecutor {

    private final Buffer.IndividualBuffer buffer;

    IrregularMovementsFoodSprint(CheckExecutor executor) {
        super(executor, "food_sprint", true);
        this.buffer = new Buffer.IndividualBuffer();
    }

    void run() {
        if (this.protocol().isSprinting()) {
            GameMode gameMode = this.protocol().player.getGameMode();
            int food = this.protocol().player.getFoodLevel(),
                    threshold = 6;

            if (food <= threshold
                    && (gameMode == GameMode.SURVIVAL
                    || gameMode == GameMode.ADVENTURE)
                    && buffer.count(1, 10) == 4) {
                buffer.reset();
                cancel(
                        "type: food-sprint"
                                + ", game-mode: " + gameMode.toString().toLowerCase().replace("_", "-")
                                + ", food: " + food,
                        threshold / (double) food,
                        this.protocol().spartanPlayer.movement.getDetectionLocation(),
                        0,
                        true
                );
            }
        }
    }

}
