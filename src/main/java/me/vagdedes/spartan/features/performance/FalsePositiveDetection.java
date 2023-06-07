package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.profiling.PlayerViolation;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FalsePositiveDetection {

    private static final String timeoutKey = "false-positive-detection";
    private static final int
            timeoutTicks = 5 * 20,
            timeoutFails = 5 * SpartanLocation.clearanceTick,
            timeoutStrictFails = 5 * (SpartanLocation.clearanceTick / 2),
            correlationSizeLimit = 65536,
            integerNearest = 5,
            decimalPoint = 1;
    private static final Map<Integer, Integer>
            decimalCorrelation = Cache.store(new ConcurrentHashMap<>(correlationSizeLimit)),
            integerCorrelation = Cache.store(new ConcurrentHashMap<>(correlationSizeLimit));

    public static boolean canFunction() {
        return Settings.getBoolean("Performance.enable_false_positive_detection");
    }

    private static boolean runTimeout(SpartanPlayer p, Enums.HackType hackType, boolean hasDecimals, boolean strict) {
        Buffer buffer = p.getBuffer();
        String key = timeoutKey + "=" + hackType.getStringOrdinal();
        int limit = strict || !hasDecimals ? timeoutStrictFails : timeoutFails;

        if (buffer.start(key, timeoutTicks) == limit
                | buffer.start(timeoutKey, timeoutTicks) == (limit * 2)) { // Call it regardless
            buffer.remove(new String[]{key, timeoutKey});
            p.getCooldowns().add(timeoutKey, timeoutTicks);
            return true;
        }
        return isTimedOut(p);
    }

    static boolean isTimedOut(Cooldowns cooldowns) {
        return !cooldowns.canDo(timeoutKey);
    }

    static boolean isTimedOut(SpartanPlayer p) {
        return isTimedOut(p.getCooldowns());
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

    public static boolean canCorrect(SpartanPlayer p,
                                     Enums.HackType hackType, ResearchEngine.DataType dataType,
                                     PlayerViolation playerViolation, boolean strict) {
        if (!canFunction() || isTimedOut(p)) {
            return false;
        }
        return ResearchEngine.isFalsePositive(
                ResearchEngine.getFalsePositives(hackType, dataType),
                playerViolation.getFalsePositiveIdentity()
        ) && !runTimeout(p, hackType, playerViolation.getDecimals().length > 0, strict);
    }
}
