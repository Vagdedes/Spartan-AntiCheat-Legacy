package me.vagdedes.spartan.objects.profiling;

import me.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.java.math.WaveProbability;

import java.util.*;

public class ViolationStatistics {

    private static final Map<Integer, Map<Integer, Integer>> violationProbabilityPie = new LinkedHashMap<>();
    private static final Map<Integer, Map<PlayerProfile, Double>> profileAverageProbability = new LinkedHashMap<>();
    private static final Map<Integer, Map<Double, Integer>> profileProbabilityWave = new LinkedHashMap<>();
    private static boolean running = false;
    private static final Object synchronizer = new Object();

    // Separator

    public static class TimePeriod {

        private final Set<Integer> players;
        private final Map<Integer, Integer> violations;

        public TimePeriod() {
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

        public Map<Integer, Double> getAverageViolations() {
            int size = this.violations.size();

            if (size > 0) {
                Map<Integer, Double> violations = new HashMap<>(size);
                double players = this.players.size();

                for (Map.Entry<Integer, Integer> entry : this.violations.entrySet()) {
                    violations.put(entry.getKey(), entry.getValue() / players);
                }
                return violations;
            } else {
                return new HashMap<>(0);
            }
        }
    }

    // Separator

    private static int hash(PlayerViolation playerViolation) {
        return Objects.hash(
                playerViolation.getLevel(),
                playerViolation.getSimilarityIdentity()
        );
    }

    // Separator

    private static void rawClear() {
        violationProbabilityPie.clear();
        profileAverageProbability.clear();
        profileProbabilityWave.clear();
    }

    public static void clear() {
        synchronized (synchronizer) {
            rawClear();
        }
    }

    // Separator

    public static void remove(Enums.HackType hackType) {
        synchronized (synchronizer) {
            for (ResearchEngine.DataType dataType : ResearchEngine.getDynamicUsableDataTypes(false)) {
                int majorHash = Objects.hash(hackType, dataType);
                violationProbabilityPie.remove(majorHash);
                profileAverageProbability.remove(majorHash);
                profileProbabilityWave.remove(majorHash);
            }
        }
    }

    public static void remove(PlayerProfile playerProfile) {
        synchronized (synchronizer) {
            ResearchEngine.DataType dataType = playerProfile.getDataType();

            for (Enums.HackType hackType : Enums.HackType.values()) {
                Map<PlayerProfile, Double> map = profileAverageProbability.get(Objects.hash(hackType, dataType));

                if (map != null) {
                    map.remove(playerProfile);
                }
            }
        }
    }

    // Separator

    public static void calculateData(Collection<PlayerProfile> playerProfiles) {
        if (running) {
            return;
        }
        running = true;

        synchronized (synchronizer) {
            if (!playerProfiles.isEmpty() && ResearchEngine.enoughData()) {
                boolean noStatistics = violationProbabilityPie.isEmpty();
                ResearchEngine.DataType[] dataTypes = ResearchEngine.getDynamicUsableDataTypes(false);

                for (Enums.HackType hackType : Enums.HackType.values()) {
                    for (ResearchEngine.DataType dataType : dataTypes) {
                        int majorHash = Objects.hash(hackType, dataType);

                        if (hackType.getCheck().isEnabled(dataType, null, null)) {
                            int totalViolations = 0;

                            for (PlayerProfile profile : playerProfiles) {
                                if ((noStatistics || profile.shouldCalculateEvidence()) && profile.getDataType() == dataType) {
                                    Collection<Collection<PlayerViolation>> violations = profile.getViolationHistory(hackType).getTimeSortedViolations();

                                    if (!violations.isEmpty()) {
                                        for (Collection<PlayerViolation> moment : violations) {
                                            for (PlayerViolation violation : moment) {
                                                if (violation.isDetectionEnabled()) {
                                                    violationProbabilityPie.computeIfAbsent(majorHash, k -> new LinkedHashMap<>()).merge(hash(violation), 1, Integer::sum);
                                                    totalViolations++;
                                                } else {
                                                    Map<Integer, Integer> map = violationProbabilityPie.get(majorHash);

                                                    if (map != null) {
                                                        map.remove(hash(violation));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (totalViolations >= ResearchEngine.logRequirement) {
                                calculateProfileProbability(
                                        playerProfiles,
                                        hackType,
                                        dataType,
                                        majorHash,
                                        totalViolations
                                );
                            }
                        } else {
                            violationProbabilityPie.remove(majorHash);
                            profileAverageProbability.remove(majorHash);
                            profileProbabilityWave.remove(majorHash);

                            for (PlayerProfile profile : playerProfiles) {
                                profile.historicalEvidence.remove(hackType);
                            }
                        }
                    }
                }
            } else {
                rawClear();

                if (!playerProfiles.isEmpty()) {
                    for (PlayerProfile profile : playerProfiles) {
                        profile.historicalEvidence.clear();
                    }
                }
            }
            running = false;
        }
    }

    private static void calculateProfileProbability(Collection<PlayerProfile> playerProfiles,
                                                    Enums.HackType hackType,
                                                    ResearchEngine.DataType dataType,
                                                    int majorHash,
                                                    int totalViolations) {
        profileAverageProbability.clear();
        profileProbabilityWave.clear();
        Map<Integer, Integer> violationProbability = violationProbabilityPie.get(majorHash);
        Map<Long, PlayerViolation> highestViolation = new LinkedHashMap<>();

        for (PlayerProfile profile : playerProfiles) {
            if (profile.getDataType() == dataType) {
                Collection<Collection<PlayerViolation>> violations = profile.getViolationHistory(hackType).getTimeSortedViolations();

                if (!violations.isEmpty()) {
                    double probabilityTotal = 0.0;
                    int probabilityCount = 0;

                    for (Collection<PlayerViolation> moment : violations) {
                        for (PlayerViolation violation : moment) {
                            if (violation.isDetectionEnabled()) {
                                long time = AlgebraUtils.floorToNearest(violation.getTime(), (int)Check.violationCycleSeconds);
                                PlayerViolation highest = highestViolation.get(time);

                                if (highest == null || violation.getLevel() > highest.getLevel()) {
                                    highestViolation.put(time, violation);
                                }
                            }
                        }
                    }

                    for (PlayerViolation violation : highestViolation.values()) {
                        probabilityTotal += violationProbability.getOrDefault(hash(violation), 0) / (double) totalViolations;
                        probabilityCount++;
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
        }
        judgeProbability(
                playerProfiles,
                hackType,
                dataType,
                majorHash
        );
    }

    private static void judgeProbability(Collection<PlayerProfile> playerProfiles,
                                         Enums.HackType hackType,
                                         ResearchEngine.DataType dataType,
                                         int majorHash) {
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
                if (profile.getDataType() == dataType) {
                    Double probability = probabilityAverage.get(profile);

                    if (probability != null) {
                        AwarenessNotifications.forcefullySend(probability.toString());
                    }
                    if (probability != null && probability <= 0.1) {
                        profile.historicalEvidence.add(hackType, "Probability: " + (wave.getPosition(probability) * 100.0) + "%");
                    } else {
                        profile.historicalEvidence.remove(hackType);
                    }
                }
            }
        }
    }
}