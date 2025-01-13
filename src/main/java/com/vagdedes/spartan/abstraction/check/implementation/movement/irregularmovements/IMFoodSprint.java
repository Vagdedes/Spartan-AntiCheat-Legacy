package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;
import com.vagdedes.spartan.abstraction.data.Buffer;
import org.bukkit.GameMode;

public class IMFoodSprint extends HardcodedDetection {

    private final Buffer.IndividualBuffer buffer;

    IMFoodSprint(CheckRunner executor) {
        super(executor, null, null, "food_sprint", true);
        this.buffer = new Buffer.IndividualBuffer();
    }

    void run() {
        this.call(() -> {
            if (this.protocol.isSprinting()) {
                GameMode gameMode = this.protocol.bukkit().getGameMode();
                int food = this.protocol.bukkit().getFoodLevel(),
                        threshold = 6;

                if (food <= threshold
                        && (gameMode == GameMode.SURVIVAL
                        || gameMode == GameMode.ADVENTURE)) {
                    double ratio = buffer.count(1, 10) / 10.0;
                    this.setHackingRatio(ratio);

                    if (ratio >= 0.4) {
                        cancel(
                                "type: food-sprint"
                                        + ", game-mode: " + gameMode.toString().toLowerCase().replace("_", "-")
                                        + ", food: " + food,
                                this.protocol.getFromLocation(),
                                0,
                                true
                        );
                    }
                } else {
                    this.setHackingRatio(0.0);
                }
            }
        });
    }

}
