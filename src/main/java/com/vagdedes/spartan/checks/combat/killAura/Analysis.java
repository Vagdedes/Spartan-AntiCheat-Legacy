package com.vagdedes.spartan.checks.combat.killAura;

// Study of the data that has been collected

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.profiling.PlayerFight;
import com.vagdedes.spartan.objects.profiling.PlayerOpponent;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Analysis extends DetectionExecutor {

    Analysis(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    private enum AnalysisType {
        CPS_AVERAGE(1),
        MAX_HIT_COMBO(0, 0),
        HITS_AVERAGE(1),
        REACH_AVERAGE(1),
        YAW_RATE_AVERAGE(1),
        PITCH_RATE_AVERAGE(1),
        HIT_TIME_AVERAGE(0, TPS.tickTimeInteger);

        private final Map<PlayerOpponent, Map<Number, Integer>> dataStorage;
        private final Map<Number, Integer> selfStorage;
        private final int decimalPlaces, integerPrecision;

        AnalysisType(int decimalPlaces, int integerPrecision) {
            this.decimalPlaces = decimalPlaces;
            this.integerPrecision = integerPrecision;
            this.dataStorage = new LinkedHashMap<>();
            this.selfStorage = new LinkedHashMap<>();
        }

        AnalysisType(int decimalPlaces) {
            this(decimalPlaces, 0);
        }

        private void clear() {
            dataStorage.clear();
            selfStorage.clear();
        }

        // Separator

        private void rest(PlayerOpponent opponent) {
            Map<Number, Integer> map = dataStorage.computeIfAbsent(opponent, k -> new LinkedHashMap<>());
            Number number;

            switch (this) {
                case CPS_AVERAGE:
                    number = manipulateDouble(opponent.getCPSAverage());
                    break;
                case MAX_HIT_COMBO:
                    number = manipulateInteger(opponent.getMaxHitCombo());
                    break;
                case HITS_AVERAGE:
                    number = manipulateDouble(opponent.getHitsAverage());
                    break;
                case REACH_AVERAGE:
                    number = manipulateDouble(opponent.getReachAverage());
                    break;
                case YAW_RATE_AVERAGE:
                    number = manipulateDouble(opponent.getYawRateAverage());
                    break;
                case PITCH_RATE_AVERAGE:
                    number = manipulateDouble(opponent.getPitchRateAverage());
                    break;
                default: // HIT_TIME
                    number = manipulateLong(opponent.getHitTimeAverage());
                    break;
            }
            map.put(
                    number,
                    map.getOrDefault(number, 0) + 1
            );
        }

        private void self(PlayerOpponent opponent) {
            Number number;

            switch (this) {
                case CPS_AVERAGE:
                    number = manipulateDouble(opponent.getCPSAverage());
                    break;
                case MAX_HIT_COMBO:
                    number = manipulateInteger(opponent.getMaxHitCombo());
                    break;
                case HITS_AVERAGE:
                    number = manipulateDouble(opponent.getHitsAverage());
                    break;
                case REACH_AVERAGE:
                    number = manipulateDouble(opponent.getReachAverage());
                    break;
                case YAW_RATE_AVERAGE:
                    number = manipulateDouble(opponent.getYawRateAverage());
                    break;
                case PITCH_RATE_AVERAGE:
                    number = manipulateDouble(opponent.getPitchRateAverage());
                    break;
                default: // HIT_TIME
                    number = manipulateLong(opponent.getHitTimeAverage());
                    break;
            }
            selfStorage.put(
                    number,
                    selfStorage.getOrDefault(number, 0) + 1
            );
        }

        // Separator

        private double getProbabilityRatioAveraged() {
            double probabilityRatios = 0.0;
            int probabilityCount = 0, totalPositions = 0;
            Map<Number, Integer> totalMap = new LinkedHashMap<>();

            // Pie Equation

            for (Map<Number, Integer> map : dataStorage.values()) {
                for (Map.Entry<Number, Integer> entry : map.entrySet()) {
                    totalMap.put(
                            entry.getKey(),
                            totalMap.getOrDefault(entry.getKey(), 0) + entry.getValue()
                    );
                    totalPositions += entry.getValue();
                }
            }

            // Probability Addition (If Found)

            for (Map.Entry<Number, Integer> selfEntry : selfStorage.entrySet()) {
                double probabilityRatio = totalMap.get(selfEntry.getKey()) / (double) totalPositions;

                for (int i = 0; i < selfEntry.getValue(); i++) { // Repeat the probability addition to the amount of times it was found
                    probabilityRatios += probabilityRatio;
                    probabilityCount++;
                }
            }

            // Probability Division (If Found)

            return probabilityRatios / (double) probabilityCount;
        }

        // Separator

        private double manipulateDouble(double number) {
            return decimalPlaces == 0 ? number : AlgebraUtils.cut(number, decimalPlaces);
        }

        private int manipulateInteger(int number) {
            return integerPrecision == 0 ? number : AlgebraUtils.roundToNearest(number, integerPrecision);
        }

        private long manipulateLong(long number) {
            return integerPrecision == 0 ? number : AlgebraUtils.roundToNearest(number, integerPrecision);
        }

        private boolean isFunctional() {
            return !dataStorage.isEmpty() && !selfStorage.isEmpty();
        }
    }

    void run(PlayerOpponent self) {
        if (!ResearchEngine.isCaching()) {
            double percentage = hackType.getCheck().getDecimalOption("analysis_percentage", 2.0);

            if (percentage > 0.0 && !ResearchEngine.getFights().isEmpty()) {
                if (percentage > 5.0) {
                    percentage = 5.0;
                }
                forceAsync(() -> {
                    List<PlayerProfile> profiles = ResearchEngine.getPlayerProfiles();

                    if (!profiles.isEmpty()) {
                        for (AnalysisType type : AnalysisType.values()) {
                            type.clear();
                        }

                        for (PlayerProfile profile : profiles) {
                            List<PlayerFight> fights = profile.getCombat().getPastFights();

                            if (!fights.isEmpty()) {
                                for (PlayerFight fight : fights) {
                                    for (PlayerOpponent opponent : fight.getOpponents()) {
                                        for (AnalysisType type : AnalysisType.values()) {
                                            type.rest(opponent);
                                        }
                                    }
                                }
                            }
                        }
                        StringBuilder debug = new StringBuilder();

                        for (AnalysisType type : AnalysisType.values()) {
                            type.self(self);

                            if (type.isFunctional()) {
                                debug.append(type.name()).append(": ").append(type.getProbabilityRatioAveraged()).append(", ");
                            } else {
                                debug.append(type.name()).append(": ").append("N/A, ");
                            }
                        }
                        //player.debug(false, false, debug.toString());
                    }
                });
            }
        }
    }
}
