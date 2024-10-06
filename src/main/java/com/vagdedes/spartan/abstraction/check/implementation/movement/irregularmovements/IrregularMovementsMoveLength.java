package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class IrregularMovementsMoveLength extends DetectionExecutor {

    private static final double
            gcdMultiplier = Math.pow(2, 24), // 16777216
            gcdLimit = 16384,
            minimumGCD = 0.005,
            safeGCD = 0.001;

    private Float
            previousPitch,
            previousPitchDifference;
    private final Buffer.IndividualBuffer buffer, safeBuffer;

    IrregularMovementsMoveLength(CheckExecutor executor) {
        super(executor, "move_length", true);
        previousPitch = executor.player.movement.getEventFromLocation().getPitch();
        previousPitchDifference = 0.0f;
        this.buffer = new Buffer.IndividualBuffer();
        this.safeBuffer = new Buffer.IndividualBuffer();
    }

    void run() {
        if (!player.isBedrockPlayer()) {
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
                    boolean safe = gcd <= safeGCD;
                    Buffer.IndividualBuffer buffer = safe ? safeBuffer : this.buffer;

                    if (buffer.count(1, 20) >= (safe ? 5 : 13)) {
                        buffer.reset();
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
                    buffer.decrease(1);
                }
            }
            previousPitchDifference = pitchDifference;
            previousPitch = pitch;
        }
    }

}
