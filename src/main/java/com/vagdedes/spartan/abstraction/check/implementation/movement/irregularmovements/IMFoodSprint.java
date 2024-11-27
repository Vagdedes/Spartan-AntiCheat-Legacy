package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.data.Buffer;
import org.bukkit.GameMode;

public class IMFoodSprint extends ProbabilityDetection {

    private final Buffer.IndividualBuffer buffer;

    IMFoodSprint(CheckRunner executor) {
        super(executor, "food_sprint", true);
        this.buffer = new Buffer.IndividualBuffer();
    }

    final void run() {
        if (this.protocol().isSprinting()) {
            GameMode gameMode = this.protocol().bukkit.getGameMode();
            int food = this.protocol().bukkit.getFoodLevel(),
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
                        this.protocol().getFromLocation(),
                        0,
                        true
                );
            }
        }
    }

}
