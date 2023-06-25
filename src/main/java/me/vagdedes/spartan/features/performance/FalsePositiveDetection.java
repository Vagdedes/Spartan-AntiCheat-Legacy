package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.PlayerViolation;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FalsePositiveDetection {

    private static final int
            correlationSizeLimit = 65536,
            integerNearest = 5,
            decimalPoint = 1;
    private static final Map<Integer, Integer>
            decimalCorrelation = Cache.store(new ConcurrentHashMap<>(correlationSizeLimit)),
            integerCorrelation = Cache.store(new ConcurrentHashMap<>(correlationSizeLimit));

    public static boolean canFunction() {
        return Settings.getBoolean("Performance.enable_false_positive_detection");
    }

    public static int getSimplifiedNumber(String information, Double[] decimals, Integer[] integers) {
        int decimalCount = decimals.length;

        if (decimalCount > 0) {
            int initialHash = information.hashCode();
            Integer correlated = decimalCorrelation.get(initialHash);

            if (correlated != null) {
                return correlated;
            }
            Set<Double> processedDecimals = new HashSet<>(decimalCount);

            for (Double decimal : decimals) {
                if (processedDecimals.add(decimal)) {
                    information = information.replace(Double.toString(decimal), String.valueOf(AlgebraUtils.cut(decimal, decimalPoint)));
                }
            }
            int integerCount = integers.length;

            if (integerCount > 0) {
                Set<Integer> processedIntegers = new HashSet<>(Math.min(decimalCount, integerCount));

                for (Integer integer : integers) {
                    if (processedIntegers.add(integer)) {
                        if (decimalCount > 0) {
                            information = information.replace(Integer.toString(integer), "");
                            decimalCount--;
                        } else {
                            information = information.replace(Integer.toString(integer), Integer.toString(AlgebraUtils.roundToNearest(integer, integerNearest)));
                        }
                    }
                }
            }
            if (decimalCorrelation.size() >= correlationSizeLimit) {
                decimalCorrelation.clear();
            }
            int simplifiedHash = information.hashCode();
            decimalCorrelation.put(initialHash, simplifiedHash);
            return simplifiedHash;
        }
        int integerCount = integers.length;

        if (integerCount > 0) {
            int initialHash = information.hashCode();
            Integer correlated = integerCorrelation.get(initialHash);

            if (correlated != null) {
                return correlated;
            }
            Set<Integer> processedIntegers = new HashSet<>(integerCount);

            for (Integer integer : integers) {
                if (processedIntegers.add(integer)) {
                    information = information.replace(Integer.toString(integer), Integer.toString(AlgebraUtils.roundToNearest(integer, integerNearest)));
                }
            }

            if (integerCorrelation.size() >= correlationSizeLimit) {
                integerCorrelation.clear();
            }
            int simplifiedHash = information.hashCode();
            integerCorrelation.put(initialHash, simplifiedHash);
            return simplifiedHash;
        }
        return information.hashCode();
    }

    public static boolean canCorrect(Check check, Enums.HackType hackType, ResearchEngine.DataType dataType,
                                     PlayerViolation playerViolation,
                                     int cancelledViolations) {
        return canFunction() // 1
                && (check.isProblematicDetection(dataType, playerViolation.getDetection(true)) // 2
                || ResearchEngine.isFalsePositive( // 2
                ResearchEngine.getFalsePositives(hackType, dataType),
                playerViolation.getFalsePositiveIdentity()))
                && cancelledViolations < check.getMaxCancelledViolations(); // 3
    }
}
