package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.PlayerViolation;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.LiveViolation;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class FalsePositiveDetection {

    private static final int
            integerNearest = 5,
            decimalPoint = 1;

    public static boolean canFunction() {
        return Settings.getBoolean("Performance.enable_false_positive_detection");
    }

    public static int getSimplifiedNumber(Enums.HackType hackType, String detection, Collection<Number> numbers) {
        int hash = (hackType.hashCode() * SpartanBukkit.hashCodeMultiplier) + detection.hashCode();

        if (numbers.size() > 0) {
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

    public static boolean canCorrect(UUID uuid,
                                     Check check, Enums.HackType hackType, ResearchEngine.DataType dataType,
                                     PlayerViolation playerViolation, LiveViolation liveViolation,
                                     boolean problematicDetections) {
        if (canFunction() // 1
                && (problematicDetections && check.isProblematicDetection(dataType, playerViolation.getDetection()) // 2
                || ResearchEngine.getFalsePositives(hackType, dataType).contains(playerViolation.getSimilarityIdentity())) // 2
                && !liveViolation.hasMaxCancelledLevel()) { // 3
            List<LiveViolation> violationList = Check.getViolationsObjects(uuid);
            violationList.remove(liveViolation);
            int count = 0;

            if (violationList.size() > 0) {
                for (LiveViolation loopLiveViolation : violationList) {
                    if (loopLiveViolation.hasMaxCancelledLevel()) {
                        count++;

                        if (count == Check.hackerCheckAmount) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }
}
