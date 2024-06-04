package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.CombatUtils;

import java.util.LinkedHashMap;

public class MoveLength extends DetectionExecutor {

    private static final double
            gcdMultiplier = Math.pow(2, 24), // 16777216
            gcdLimit = 16384,
            minimumGCD = 0.005,
            safeGCD = 0.001;

    private Float
            previousPitch,
            previousPitchDifference;
    private final Buffer buffer;

    MoveLength(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        previousPitch = executor.player.movement.getEventFromLocation().getPitch();
        previousPitchDifference = 0.0f;
        this.buffer = new Buffer(player, new LinkedHashMap<>());
    }

    void runMove() {
        if (!player.bedrockPlayer
                && player.getLastDamageDealt().ticksPassed() <= KillAura.pastDamageMilliseconds
                && !player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance).isEmpty()) {
            float pitch = player.movement.getLocation().getPitch(),
                    pitchDifference = Math.abs(pitch - previousPitch);

            if (pitchDifference > 0.0
                    && this.previousPitchDifference > 0.0) {
                double gcd = AlgebraUtils.findGCD(
                        gcdLimit,
                        pitchDifference * gcdMultiplier,
                        this.previousPitchDifference * gcdMultiplier
                ) / gcdMultiplier;

                if (gcd <= minimumGCD) {
                    String key = gcd <= safeGCD ? "safe" : "default";

                    if (gcd <= safeGCD ?
                            buffer.count(key, 20) >= 5 :
                            buffer.count(key, 20) >= 13) {
                        buffer.remove(key);
                        cancel(
                                "type: move-length"
                                + ", pitch: " + pitch
                                + ", previous-pitch: " + previousPitch
                                + ", pitch-difference: " + pitchDifference
                                + ", previous-pitch-difference: " + this.previousPitchDifference
                                + ", greatest-common-divisor: " + gcd
                                + ", limit: " + minimumGCD,
                                1.0,
                                null,
                                (int) TPS.maximum
                        );
                    }
                } else { // Some players may false this due to their client, so this helps prevent it
                    buffer.decrease("default", 1);
                }
            }
            previousPitchDifference = pitchDifference;
            previousPitch = pitch;
        }
    }

}
