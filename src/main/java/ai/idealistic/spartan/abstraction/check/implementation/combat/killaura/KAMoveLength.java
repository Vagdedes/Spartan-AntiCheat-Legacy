package ai.idealistic.spartan.abstraction.check.implementation.combat.killaura;

import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.data.Buffer;
import ai.idealistic.spartan.abstraction.world.ServerLocation;
import ai.idealistic.spartan.functionality.server.TPS;
import ai.idealistic.spartan.utils.math.AlgebraUtils;
import ai.idealistic.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Material;

public class KAMoveLength extends CheckDetection {

    private static final double
            gcdMultiplier = Math.pow(2, 24),
            gcdLimit = 16384,
            minimumGCD = 0.005,
            safeGCD = 0.001;

    private Float
            previousPitch,
            previousPitchDifference;
    private final Buffer.IndividualBuffer buffer, safeBuffer;
    private long hit, hitBy;

    KAMoveLength(CheckRunner executor) {
        super(executor, Check.DataType.JAVA, Check.DetectionType.PACKETS, "move_length", true,
                DEFAULT_AVERAGE_TIME * 4L,
                TIME_TO_NOTIFY,
                TIME_TO_PREVENT,
                TIME_TO_PUNISH);
        previousPitch = 0.0f;
        previousPitchDifference = 0.0f;
        this.buffer = new Buffer.IndividualBuffer();
        this.safeBuffer = new Buffer.IndividualBuffer();
    }

    void run() {
        this.call(
                () -> {
                    Material currentMat = new ServerLocation(this.protocol.getLocation()).getBlock(this.protocol).getTypeOrNull();
                    if (currentMat != null && BlockUtils.areWebs(currentMat)) {
                        return;
                    }

                    // EXTENSION: Exempt client-side cinematic smoothing features that destroy the sensitivity multiplier
                    if (this.protocol.isSwimming() || this.protocol.isGliding() || this.protocol.getVehicle() != null) {
                        return;
                    }

                    long now = System.currentTimeMillis();

                    // EXTENSION: Fixed the inverted math. The timer now properly expires after 2 seconds.
                    // Changed AND (&&) to OR (||) so it catches players who are hitting OR being hit.
                    if (now - this.hit <= 2_000L || now - this.hitBy <= 1_000L) {

                        float pitch = this.protocol.getLocation().getPitch();
                        float pitchDifference = Math.abs(pitch - previousPitch);

                        // EXTENSION: Ignore microscopic floating-point noise (< 0.01) which naturally bypasses GCD math
                        if (pitchDifference > 0.01f && this.previousPitchDifference > 0.01f) {

                            double gcd = AlgebraUtils.findGCD(
                                    gcdLimit,
                                    pitchDifference * gcdMultiplier,
                                    this.previousPitchDifference * gcdMultiplier
                            ) / gcdMultiplier;

                            if (gcd <= minimumGCD) {
                                boolean safe = gcd <= safeGCD;
                                Buffer.IndividualBuffer activeBuffer = safe ? safeBuffer : this.buffer;

                                if (activeBuffer.count(1, 20) >= (safe ? 5 : 13)) {
                                    activeBuffer.reset();
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
                            } else {
                                // Drain both buffers if the player proves they are using human mouse movements
                                this.buffer.decrease(0.5);
                                this.safeBuffer.decrease(0.5);
                            }
                        }
                        previousPitchDifference = pitchDifference;
                        previousPitch = pitch;
                    }
                },
                () -> {
                    this.previousPitch = 0.0f;
                    this.previousPitchDifference = 0.0f;
                    this.hitBy = 0L;
                    this.hit = 0L;
                    this.buffer.reset();
                    this.safeBuffer.reset();
                }
        );
    }

    void runHit() {
        this.call(() -> this.hit = System.currentTimeMillis());
    }

    void runHitBy() {
        this.call(() -> this.hitBy = System.currentTimeMillis());
    }
}