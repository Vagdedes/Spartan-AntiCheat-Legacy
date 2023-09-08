package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.profiling.PlayerViolation;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.LiveViolation;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.*;

public class FalsePositiveDetection {

    public static class TimePeriod {

        private final long ticks;
        private final Set<Integer> players;
        private final Map<Integer, Integer> violations;

        public TimePeriod(long duration) {
            this.ticks = (int) (duration / TPS.tickTimeInteger);
            this.players = new HashSet<>();
            this.violations = new HashMap<>();
        }

        public void add(PlayerViolation playerViolation) {
            players.add(playerViolation.getPlayerIdentity());
            violations.put(
                    playerViolation.getSimilarityIdentity(),
                    violations.getOrDefault(playerViolation.getSimilarityIdentity(), 0) + 1
            );
        }

        public Map<Integer, Integer> getAverageViolations() {
            if (!violations.isEmpty()) {
                double players = this.players.size();

                for (Map.Entry<Integer, Integer> entry : violations.entrySet()) {
                    entry.setValue(AlgebraUtils.integerCeil(entry.getValue() / players / ticks)); // Ceil to be safe
                }
            }
            return violations;
        }
    }

    private static final int
            integerNearest = 5,
            decimalPoint = 1;

    public static boolean canFunction() {
        return Settings.getBoolean("Performance.enable_false_positive_detection");
    }

    public static int getSimplifiedNumber(Enums.HackType hackType, String detection, Collection<Number> numbers) {
        int hash = (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + detection.hashCode();

        if (!numbers.isEmpty()) {
            for (Number number : numbers) {
                if (number instanceof Double) {
                    hash = (hash * SpartanBukkit.hashCodeMultiplier)
                            + Double.hashCode(AlgebraUtils.cut(number.doubleValue(), decimalPoint));
                } else {
                    hash = (hash * SpartanBukkit.hashCodeMultiplier)
                            + AlgebraUtils.roundToNearest(number.intValue(), integerNearest);
                }
            }
        }
        return hash;
    }

    public static boolean canCorrect(Check check, Enums.HackType hackType, ResearchEngine.DataType dataType,
                                     PlayerViolation playerViolation, LiveViolation liveViolation,
                                     boolean problematicDetections) {
        return canFunction() // 1
                && !liveViolation.hasMaxCancelledLevel(playerViolation.getSimilarityIdentity()) // 2
                && (problematicDetections && check.isProblematicDetection(dataType, playerViolation.getDetection()) // 3
                || ResearchEngine.getFalsePositives(hackType, dataType).contains(playerViolation.getSimilarityIdentity())); // 3
    }
}
