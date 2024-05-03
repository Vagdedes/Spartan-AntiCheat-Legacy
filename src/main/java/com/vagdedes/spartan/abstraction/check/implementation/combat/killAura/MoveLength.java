package com.vagdedes.spartan.abstraction.check.implementation.combat.killAura;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.entity.Entity;

import java.util.List;

public class MoveLength extends DetectionExecutor {

    private final KillAura killAura;
    private Float
            previousPitch,
            previousPitchDifference;

    MoveLength(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        killAura = (KillAura) executor;
        previousPitch = executor.player.movement.getCustomFromLocation().getPitch();
        previousPitchDifference = 0.0f;
    }

    private static final String key = MoveLength.class.getName();
    private static final double
            gcdMultiplier = Math.pow(2, 24), // 16777216
            gcdLimit = 16384,
            minimumGCD = 0.005,
            safeGCD = 0.001;

    void run(SpartanLocation location, Buffer buffer) { // todo improve
        if (!player.bedrockPlayer) {
            if (player.getLastDamageDealt().ticksPassed() <= KillAura.pastDamageMilliseconds) {
                List<Entity> entities = player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

                if (entities.size() == 1) {
                    float pitch = location.getPitch();

                    if (Math.abs(pitch) <= 90.0f) {
                        float pitchDifference = Math.abs(pitch - previousPitch);

                        if (pitchDifference > 0.0
                                && this.previousPitchDifference > 0.0) {
                            double gcd = AlgebraUtils.findGCD(gcdLimit, pitchDifference * gcdMultiplier, this.previousPitchDifference * gcdMultiplier) / gcdMultiplier;

                            if (gcd <= minimumGCD) {
                                if (gcd <= safeGCD ?
                                        buffer.start(key + "=safe", 20, Check.detectionMeasurementTicks) >= 0.25 :
                                        buffer.start(key, 20, Check.detectionMeasurementTicks) >= 0.65) {
                                    buffer.remove(key);
                                    killAura.violateLive("type: move-time"
                                            + ", pitch: " + pitch
                                            + ", previous-pitch: " + previousPitch
                                            + ", pitch-difference: " + pitchDifference
                                            + ", previous-pitch-difference: " + this.previousPitchDifference
                                            + ", greatest-common-divisor: " + gcd
                                            + ", limit: " + minimumGCD);
                                }
                            } else { // Some players may false this due to their client, so this helps prevent it
                                buffer.decrease(key, 1);
                            }
                        }
                        previousPitchDifference = pitchDifference;
                        previousPitch = pitch;
                    }
                }
            }
        }
    }

}
