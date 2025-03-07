package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public class MoveLength extends CheckDetection {

    private static final double
            gcdMultiplier = Math.pow(2, 24), // 16777216
            gcdLimit = 16384,
            minimumGCD = 0.005,
            safeGCD = 0.001;

    private Float
            previousPitch,
            previousPitchDifference;
    private final Buffer.IndividualBuffer buffer, safeBuffer;
    private long hit, hitBy;

    MoveLength(CheckRunner executor) {
        super(executor, Check.DataType.JAVA, Check.DetectionType.PACKETS, "move_length", true);
        previousPitch = 0.0f;
        previousPitchDifference = 0.0f;
        this.buffer = new Buffer.IndividualBuffer();
        this.safeBuffer = new Buffer.IndividualBuffer();
    }

    void run() {
        this.call(() -> {
            if (this.hit - System.currentTimeMillis() <= 2_000L
                    && this.hitBy - System.currentTimeMillis() <= 1_000L) {
                float pitch = this.protocol.bukkitExtra.getLocation().getPitch(),
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
                                    "move-length"
                                            + ", pitch: " + pitch
                                            + ", previous-pitch: " + previousPitch
                                            + ", pitch-difference: " + pitchDifference
                                            + ", previous-pitch-difference: " + this.previousPitchDifference
                                            + ", greatest-common-divisor: " + gcd
                                            + ", limit: " + minimumGCD,
                                    null,
                                    AlgebraUtils.integerCeil(TPS.maximum)
                            );
                        }
                    } else { // Some players may false this due to their client, so this helps prevent it
                        buffer.decrease(1);
                    }
                }
                previousPitchDifference = pitchDifference;
                previousPitch = pitch;
            }
        });
    }

    void runHit() {
        this.call(() -> this.hit = System.currentTimeMillis());
    }

    void runHitBy() {
        this.call(() -> this.hitBy = System.currentTimeMillis());
    }

}
