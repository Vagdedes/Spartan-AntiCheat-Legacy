package ai.idealistic.spartan.abstraction.check.implementation.movement.irregularmovements;

import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import org.bukkit.GameMode;

public class IMFoodSprint extends CheckDetection {

    private long lastHighFoodTime = System.currentTimeMillis();
    private double vl = 0;

    IMFoodSprint(CheckRunner executor) {
        super(executor, Check.DataType.JAVA, null, "food_sprint", false);
    }

    void run() {
        this.call(() -> {
            int food = this.protocol.bukkit().getFoodLevel();
            int threshold = 6;

            // EXTENSION: Track the exact millisecond the player's food was last healthy
            if (food > threshold) {
                this.lastHighFoodTime = System.currentTimeMillis();
            }

            if (this.protocol.isSprinting()
                    && !this.protocol.wasGliding()
                    && !this.protocol.isGliding() // Fix 3: Active Elytra exemption
                    && this.protocol.getVehicle() == null) { // Fix 3: Vehicle exemption

                GameMode gameMode = this.protocol.getGameMode();

                if (food <= threshold
                        && (gameMode == GameMode.SURVIVAL
                        || gameMode == GameMode.ADVENTURE)) {

                    // Fix 1: The Latency Grace Period.
                    // Give the client 1000ms to process the server's food drop packet.
                    long timeSinceFoodDrop = System.currentTimeMillis() - this.lastHighFoodTime;
                    if (timeSinceFoodDrop < 1000L) {
                        return;
                    }

                    // Fix 2: Dynamic VL Accumulator to catch "Pulse Toggling" bypasses
                    this.vl += 1.0;

                    if (this.vl > 4.5) {
                        cancel(
                                "food-sprint"
                                        + ", game-mode: " + gameMode.toString().toLowerCase().replace("_", "-")
                                        + ", food: " + food
                                        + ", ms-since-drop: " + timeSinceFoodDrop,
                                this.protocol.getFromLocation(),
                                0,
                                true
                        );
                        // Do not reset VL to 0, otherwise togglers get a free restart window.
                        this.vl = 3.0;
                    }
                } else {
                    this.vl = Math.max(0, this.vl - 0.25);
                }
            } else {
                // Natural decay for legitimate walking/standing
                this.vl = Math.max(0, this.vl - 0.5);
            }
        });
    }

}