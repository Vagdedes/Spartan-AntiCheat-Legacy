package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.java.math.WaveProbability;

import java.util.*;

public class NewViolationStatistics {

    private static final Map<Integer, Map<PlayerProfile, Map<Integer, Integer>>> violationProbabilityPie = new LinkedHashMap<>();
    private static final Map<Integer, Map<PlayerProfile, Double>> profileAverageProbability = new LinkedHashMap<>();
    private static final Map<Integer, Map<Double, Integer>> profileProbabilityWave = new LinkedHashMap<>();

    public static synchronized void calculateData(Collection<PlayerProfile> playerProfiles) {
        if (!playerProfiles.isEmpty() && ResearchEngine.enoughData()) {
            boolean noStatistics = violationProbabilityPie.isEmpty();
            ResearchEngine.DataType[] dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);

            for (Enums.HackType hackType : Enums.HackType.values()) {
                for (ResearchEngine.DataType dataType : dataTypes) {
                    int majorHash = Objects.hash(hackType, dataType);

                    if (hackType.getCheck().isEnabled(dataType, null, null)) {
                        int totalViolations = 0;

                        for (PlayerProfile profile : playerProfiles) {
                            if ((noStatistics || profile.wasRecentlyOnline()) && profile.getDataType() == dataType) {
                                Collection<Collection<PlayerViolation>> violations = profile.getViolationHistory(hackType).getTimeSortedViolations();

                                if (!violations.isEmpty()) {
                                    long previousTime = Long.MIN_VALUE;

                                    for (Collection<PlayerViolation> moment : violations) {
                                        for (PlayerViolation violation : moment) {
                                            if (previousTime == Long.MIN_VALUE) {
                                                previousTime = violation.getTime();
                                            } else if (violation.isDetectionEnabled()) {
                                                int hash = Objects.hash(
                                                        violation.getLevel(),
                                                        violation.getSimilarityIdentity(),
                                                        AlgebraUtils.roundToNearest(violation.getTime() - previousTime, TPS.tickTimeInteger)
                                                );
                                                previousTime = violation.getTime();
                                                Map<PlayerProfile, Map<Integer, Integer>> map = violationProbabilityPie.computeIfAbsent(majorHash, k -> new LinkedHashMap<>());
                                                map.computeIfAbsent(profile, k -> new LinkedHashMap<>()).merge(hash, 1, Integer::sum);
                                                totalViolations++;
                                            } else {
                                                int hash = Objects.hash(
                                                        violation.getLevel(),
                                                        violation.getSimilarityIdentity(),
                                                        AlgebraUtils.roundToNearest(violation.getTime() - previousTime, TPS.tickTimeInteger)
                                                );
                                                Map<PlayerProfile, Map<Integer, Integer>> map = violationProbabilityPie.get(majorHash);

                                                if (map != null) {
                                                    Map<Integer, Integer> map2 = map.get(profile);

                                                    if (map2 != null) {
                                                        map2.remove(hash);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Map<PlayerProfile, Map<Integer, Integer>> map = violationProbabilityPie.get(majorHash);

                                    if (map != null) {
                                        map.remove(profile);
                                    }
                                }
                            }
                        }
                        calculateProfileProbability(
                                playerProfiles,
                                hackType,
                                dataType,
                                totalViolations
                        );
                    } else {
                        violationProbabilityPie.remove(majorHash);
                        profileAverageProbability.remove(majorHash);
                        profileProbabilityWave.remove(majorHash);
                    }
                }
            }
        } else {
            violationProbabilityPie.clear();
            profileAverageProbability.clear();
            profileProbabilityWave.clear();
        }
    }

    private static void calculateProfileProbability(Collection<PlayerProfile> playerProfiles,
                                                    Enums.HackType hackType,
                                                    ResearchEngine.DataType dataType,
                                                    int totalViolations) {
        profileProbabilityWave.clear();
        int majorHash = Objects.hash(hackType, dataType);
        Map<PlayerProfile, Map<Integer, Integer>> allData = violationProbabilityPie.get(majorHash);

        if (allData != null) {
            Map<Integer, Integer> onlyData = new LinkedHashMap<>();

            for (Map<Integer, Integer> map : allData.values()) {
                for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                    onlyData.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
            for (PlayerProfile profile : playerProfiles) {
                Collection<Collection<PlayerViolation>> violations = profile.getViolationHistory(hackType).getTimeSortedViolations();

                if (!violations.isEmpty()) {
                    double probabilityTotal = 0.0;
                    int probabilityCount = 0;
                    long previousTime = Long.MIN_VALUE;

                    for (Collection<PlayerViolation> moment : violations) {
                        for (PlayerViolation violation : moment) {
                            if (previousTime == Long.MIN_VALUE || !violation.isDetectionEnabled()) {
                                previousTime = violation.getTime();
                            } else {
                                int hash = Objects.hash(
                                        violation.getLevel(),
                                        violation.getSimilarityIdentity(),
                                        AlgebraUtils.roundToNearest(violation.getTime() - previousTime, TPS.tickTimeInteger)
                                );
                                previousTime = violation.getTime();
                                probabilityTotal += onlyData.getOrDefault(hash, 0) / (double) totalViolations;
                                probabilityCount++;
                            }
                        }
                    }

                    if (probabilityCount > 0) {
                        double averageProbability = probabilityTotal / (double) probabilityCount;
                        profileAverageProbability.computeIfAbsent(majorHash, k -> new LinkedHashMap<>()).put(profile, averageProbability);
                        profileProbabilityWave.computeIfAbsent(majorHash, k -> new LinkedHashMap<>()).merge(averageProbability, 1, Integer::sum);
                    } else {
                        Map<PlayerProfile, Double> map = profileAverageProbability.get(majorHash);

                        if (map != null) {
                            map.remove(profile);
                        }
                    }
                } else {
                    Map<PlayerProfile, Double> map = profileAverageProbability.get(majorHash);

                    if (map != null) {
                        map.remove(profile);
                    }
                }
            }
            judgeProbability(
                    playerProfiles,
                    hackType,
                    dataType
            );
        } else {
            profileAverageProbability.remove(majorHash);
            profileProbabilityWave.remove(majorHash);
        }
    }

    private static void judgeProbability(Collection<PlayerProfile> playerProfiles,
                                         Enums.HackType hackType,
                                         ResearchEngine.DataType dataType) {
        int majorHash = Objects.hash(hackType, dataType);
        Map<Double, Integer> probabilityData = profileProbabilityWave.get(majorHash);

        if (probabilityData != null) {
            Map<PlayerProfile, Double> probabilityAverage = profileAverageProbability.get(majorHash);
            WaveProbability wave = new WaveProbability();

            for (Map.Entry<Double, Integer> entry : probabilityData.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    wave.addNumber(entry.getKey());
                }
            }
            for (PlayerProfile profile : playerProfiles) {
                Double probability = probabilityAverage.get(profile);

                if (probability != null && profile.isSuspectedOrHacker()) {
                    AwarenessNotifications.forcefullySend(
                            profile.getName()
                                    + "(" + hackType.toString() + "): "
                                    + wave.getPosition(probability)
                    );
                }
            }
        }
    }
}
