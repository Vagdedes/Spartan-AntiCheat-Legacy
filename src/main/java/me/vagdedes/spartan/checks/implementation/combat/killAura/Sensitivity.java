package me.vagdedes.spartan.checks.implementation.combat.killAura;

// Study of the GCD (Greatest Common Divisor) of pitch movement

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.entity.Entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Sensitivity {

    private static final Map<UUID, Float>
            direction = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers())),
            calculations = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
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

    static void remove(SpartanPlayer player) {
        UUID uuid = player.getUniqueId();
        direction.remove(uuid);
        calculations.remove(uuid);
    }

    static void run(SpartanPlayer player, SpartanLocation location, Buffer buffer) {
        if (!player.isBedrockPlayer()) {
            boolean suspectedOrHacker = player.getProfile().isSuspectedOrHacker(KillAura.check);
            Decimals decimals = player.getDecimals(KillAura.check);

            if (Damage.getLastDealt(player) <= KillAura.pastDamageMilliseconds) {
                List<Entity> entities = player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

                if (entities.size() == 1) {
                    float pitch = location.getPitch();

                    if (Math.abs(pitch) <= 90.0f) {
                        UUID uuid = player.getUniqueId();
                        Float previousPitch = direction.get(uuid);

                        if (previousPitch != null) {
                            float pitchDifference = Math.abs(pitch - previousPitch);

                            if (pitchDifference > 0.0) {
                                Float previousPitchDifference = calculations.get(uuid);

                                if (previousPitchDifference != null && previousPitchDifference > 0.0) {
                                    double gcd = AlgebraUtils.findGCD(gcdLimit, pitchDifference * gcdMultiplier, previousPitchDifference * gcdMultiplier) / gcdMultiplier;

                                    if (gcd <= minimumGCD) {
                                        if (gcd <= safeGCD ?
                                                buffer.start(Buffer.BufferType.Combat, key + "=safe", 20) == 5 :
                                                buffer.start(Buffer.BufferType.Combat, key, 20) == 13) { // 2/3rds
                                            buffer.remove(key);
                                            KillAura.violateLive(player, "type: sensitivity(minimum)"
                                                    + ", pitch: " + pitch
                                                    + ", previous-pitch: " + previousPitch
                                                    + ", pitch-difference: " + pitchDifference
                                                    + ", previous-pitch-difference: " + previousPitchDifference
                                                    + ", greatest-common-divisor: " + gcd
                                                    + ", limit: " + minimumGCD);
                                        }
                                    } else if (suspectedOrHacker) {
                                        runAbstract(player, decimals, 2, gcd);
                                    } else { // Some players may false this due to their client, so this helps prevent it
                                        buffer.decrease(Buffer.BufferType.Combat, key, 1);
                                    }
                                }
                            }
                            calculations.put(uuid, pitchDifference);
                        }
                        direction.put(uuid, pitch);
                    }
                }
            } else if (suspectedOrHacker) {
                runAbstract(player, decimals, 1, Double.MAX_VALUE);
            }
        }
    }

    private static void runAbstract(SpartanPlayer player, Decimals decimals, int multiplier, double insertGCD) {
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
            KillAura.violateLive(player, prevention.toString());
        } else if (multiplier == 1) {
            decimals.remove(key);
        }
    }
}
