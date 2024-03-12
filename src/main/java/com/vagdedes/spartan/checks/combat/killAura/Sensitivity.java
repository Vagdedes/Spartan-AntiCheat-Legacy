package com.vagdedes.spartan.checks.combat.killAura;

// Study of the GCD (Greatest Common Divisor) of pitch movement

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.entity.Entity;

import java.util.List;

public class Sensitivity extends DetectionExecutor {

    private final KillAura killAura;

    Sensitivity(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        killAura = (KillAura) executor;
    }

    private Float
            previousPitch = null,
            previousPitchDifference = null;

    private static final String key = Sensitivity.class.getName();
    private static final double
            gcdMultiplier = Math.pow(2, 24), // 16777216
            gcdLimit = 16384,
            minimumGCD = 0.005,
            safeGCD = 0.001,
            requiredStabilityRatio = 0.5; // Normally we should expect nothing less than 0.9, but due to different clients being used, we lower it to half, aka 0.5
    private static final int
            requiredGCDs = 5,
            maxFailedDivisions = 3;

    void remove() {
        previousPitch = null;
        previousPitchDifference = null;
    }

    void run(SpartanLocation location, Buffer buffer) {
        if (!player.bedrockPlayer) {
            boolean suspectedOrHacker = player.getProfile().isSuspectedOrHacker(hackType);
            Decimals decimals = player.getDecimals(hackType);

            if (Damage.getLastDealt(player) <= KillAura.pastDamageMilliseconds) {
                List<Entity> entities = player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

                if (entities.size() == 1) {
                    float pitch = location.getPitch();

                    if (Math.abs(pitch) <= 90.0f) {
                        if (previousPitch != null) {
                            float pitchDifference = Math.abs(pitch - previousPitch);

                            if (pitchDifference > 0.0) {
                                if (this.previousPitchDifference != null
                                        && this.previousPitchDifference > 0.0) {
                                    double gcd = AlgebraUtils.findGCD(gcdLimit, pitchDifference * gcdMultiplier, this.previousPitchDifference * gcdMultiplier) / gcdMultiplier;

                                    if (gcd <= minimumGCD) {
                                        if (gcd <= safeGCD ?
                                                buffer.start(key + "=safe", 20) == 5 :
                                                buffer.start(key, 20) == 13) { // 2/3rds
                                            buffer.remove(key);
                                            killAura.violateLive("type: sensitivity(minimum)"
                                                    + ", pitch: " + pitch
                                                    + ", previous-pitch: " + previousPitch
                                                    + ", pitch-difference: " + pitchDifference
                                                    + ", previous-pitch-difference: " + this.previousPitchDifference
                                                    + ", greatest-common-divisor: " + gcd
                                                    + ", limit: " + minimumGCD);
                                        }
                                    } else if (suspectedOrHacker) {
                                        runAbstract(decimals, 2, gcd);
                                    } else { // Some players may false this due to their client, so this helps prevent it
                                        buffer.decrease(key, 1);
                                    }
                                }
                            }
                            previousPitchDifference = pitchDifference;
                        }
                        previousPitch = pitch;
                    }
                }
            } else if (suspectedOrHacker) {
                runAbstract(decimals, 1, Double.MAX_VALUE);
            }
        }
    }

    private void runAbstract(Decimals decimals, int multiplier, double insertGCD) {
        if (insertGCD != Double.MAX_VALUE) {
            decimals.add(key, insertGCD);
        }
        List<Double> gcds = decimals.getOldestToNewestList(key);
        int gcdsSize = gcds.size();

        if (gcdsSize >= (requiredGCDs * multiplier)) {
            decimals.remove(key);
            double[] ratios = new double[3];

            for (int i = 0; i < maxFailedDivisions; i++) {
                int successes = 0;
                double minGCD = Double.MAX_VALUE;

                for (double gcd : gcds) {
                    minGCD = Math.min(minGCD, gcd);
                }
                gcds.remove(minGCD);
                gcdsSize--;

                for (double gcd : gcds) {
                    if (gcd % minGCD < 0.0001) {
                        successes++;
                    }
                }
                double successRatio = successes / ((double) gcdsSize);

                if (successRatio < requiredStabilityRatio) {
                    ratios[i] = successRatio;

                    if (i > 0 && (i + 1) < maxFailedDivisions && ratios[i - 1] > successRatio) { // Break when the division has gotten worse
                        break;
                    }
                } else {
                    return;
                }
            }

            StringBuilder prevention = new StringBuilder(
                    "type: sensitivity(abstract)"
                            + ", comparisons: " + (gcdsSize + maxFailedDivisions)
                            + ", required-comparisons: " + requiredGCDs
                            + ", required-comparison-multiplier: " + multiplier
                            + ", max-failed-divisions: " + maxFailedDivisions
                            + ", required-ratio: " + requiredStabilityRatio
            );
            gcdsSize = 0;

            for (double ratio : ratios) {
                gcdsSize++;
                prevention.append(", recorded-ratio-").append(gcdsSize).append(": ").append(ratio);
            }
            killAura.violateLive(prevention.toString());
        } else if (multiplier == 1) {
            decimals.remove(key);
        }
    }
}
