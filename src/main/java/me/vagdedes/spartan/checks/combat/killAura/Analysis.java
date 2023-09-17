package me.vagdedes.spartan.checks.combat.killAura;

// Study of the data that has been collected

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.PlayerCombat;
import me.vagdedes.spartan.objects.profiling.PlayerFight;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class Analysis extends DetectionExecutor {

    private final KillAura killAura;

    Analysis(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        killAura = (KillAura) executor;
    }

    private static final String
            winsToLossesKey = "wins-to-losses",
            hitRatioKey = "hit-ratio";
    private static final int
            criteria,
            recordedFights = 50,
            decimalPoint = 2;
    private static final double requiredScore;
    private static final Map<String, Double> weight;

    static {
        // All weights must add up to the map size
        // greater the number = more important
        // 1.1: Needs more attention
        // 1.0: Can be compared properly
        // 0.9: Risky so degrade its weight
        weight = new LinkedHashMap<>(9);
        weight.put(PlayerFight.reachAverageKey, 1.0);
        weight.put(PlayerFight.maxCpsKey, 1.1);
        weight.put(PlayerFight.hitTimeAverageKey, 0.9);
        weight.put(PlayerFight.maxHitComboKey, 1.1);
        weight.put(PlayerFight.durationKey, 0.9);
        weight.put(PlayerFight.pitchRateKey, 1.0);
        weight.put(PlayerFight.yawRateKey, 1.0);
        weight.put(winsToLossesKey, 0.9);
        weight.put(hitRatioKey, 1.1);

        criteria = weight.size();
        requiredScore = criteria * 2.0;
    }

    private double calculatePoints(String key, double average, double globalAverage, double globalMaxAverage) {
        return globalAverage == -1.0 || globalMaxAverage == -1.0 ? 0.0
                : ((average / globalAverage) + (average / globalMaxAverage)) * weight.get(key);
    }

    private double calculatePointsReverse(String key, double average, double globalAverage, double globalMaxAverage) {
        return globalAverage == -1.0 || globalMaxAverage == -1.0 ? 0.0
                : ((globalAverage / average) + (globalMaxAverage / average)) * weight.get(key);
    }

    void run() {
        if (!ResearchEngine.isCaching()) {
            int percentage = hackType.getCheck().getNumericalOption("analysis_percentage", 25);

            if (percentage > 0 && ResearchEngine.getFights().size() >= recordedFights) {
                if (percentage > 1000) {
                    percentage = 1000;
                }
                double requiredScore = Analysis.requiredScore * (1.0 + (percentage / 100.0));

                if (player != null) {
                    PlayerCombat combat = player.getProfile().getCombat();

                    if (combat.hasFights()) {
                        double points = 0.0,
                                winsToLossesRatio = combat.getAverageWinLossRatio(), winToLossesRatioAgainst, winToLossesMaxRatioAgainst,
                                reachRatio, reachRatioAgainst, reachMaxRatioAgainst,
                                hitTimeRatio, hitTimeRatioAgainst, hitTimeMaxRatioAgainst,
                                hitComboRatio, hitComboRatioAgainst, hitComboMaxRatioAgainst,
                                hitRatioRatio, hitRatioRatioAgainst, hitRatioMaxRatioAgainst,
                                durationRatio, durationRatioAgainst, durationMaxRatioAgainst,
                                pitchRateRatio, pitchRateRatioAgainst, pitchRateMaxRatioAgainst,
                                yawRateRatio, yawRateRatioAgainst, yawRateMaxRatioAgainst,
                                cpsRatio, cpsRatioAgainst, cpsMaxRatioAgainst;
                        double[] winsToLossesAverages = ResearchEngine.getWinsToLosses(),
                                reach = combat.getReachAverages(),
                                reachAverages = ResearchEngine.getReach(),
                                hitTime = combat.getHitTimeAverages(),
                                hitTimeAverages = ResearchEngine.getHitTime(),
                                hitCombo = combat.getHitComboAverages(),
                                hitComboAverages = ResearchEngine.getHitCombo(),
                                hitRatio = combat.getHitRatioAverages(),
                                hitRatioAverages = ResearchEngine.getHitRatio(),
                                duration = combat.getDurationAverages(),
                                durationAverages = ResearchEngine.getDuration(),
                                pitchRate = combat.getPitchRateAverages(),
                                pitchRateAverages = ResearchEngine.getPitchRate(),
                                yawRate = combat.getYawRateAverages(),
                                yawRateAverages = ResearchEngine.getYawRate(),
                                cps = combat.getCPSAverages(),
                                cpsAverages = ResearchEngine.getCPS();

                        if (winsToLossesRatio != -1.0) {
                            winToLossesRatioAgainst = Math.max(winsToLossesAverages[1], 1.5);
                            winToLossesMaxRatioAgainst = Math.max(winsToLossesAverages[2], 2.0);
                            points += calculatePoints(winsToLossesKey, winsToLossesRatio, winToLossesRatioAgainst, winToLossesMaxRatioAgainst);
                        } else {
                            winToLossesRatioAgainst = -1.0;
                            winToLossesMaxRatioAgainst = -1.0;
                        }

                        if (reach != null) {
                            reachRatio = reach[1];
                            reachRatioAgainst = Math.max(reachAverages[1], 2.5);
                            reachMaxRatioAgainst = Math.max(reachAverages[2], CombatUtils.maxLegitimateHitDistance);
                            points += calculatePoints(PlayerFight.reachAverageKey, reachRatio, reachRatioAgainst, reachMaxRatioAgainst);
                        } else {
                            reachRatio = -1.0;
                            reachRatioAgainst = -1.0;
                            reachMaxRatioAgainst = -1.0;
                        }

                        if (hitTime != null) {
                            hitTimeRatio = hitTime[1];
                            hitTimeRatioAgainst = Math.max(hitTimeAverages[1], 1.0);
                            hitTimeMaxRatioAgainst = Math.max(hitTimeAverages[2], 1.0);
                            points += calculatePointsReverse(PlayerFight.hitTimeAverageKey, hitTimeRatio, hitTimeRatioAgainst, hitTimeMaxRatioAgainst);
                        } else {
                            hitTimeRatio = -1.0;
                            hitTimeRatioAgainst = -1.0;
                            hitTimeMaxRatioAgainst = -1.0;
                        }

                        if (hitCombo != null) {
                            hitComboRatio = hitCombo[1];
                            hitComboRatioAgainst = Math.max(hitComboAverages[1], 2.0);
                            hitComboMaxRatioAgainst = Math.max(hitComboAverages[2], 3.0);
                            points += calculatePoints(PlayerFight.maxHitComboKey, hitComboRatio, hitComboRatioAgainst, hitComboMaxRatioAgainst);
                        } else {
                            hitComboRatio = -1.0;
                            hitComboRatioAgainst = -1.0;
                            hitComboMaxRatioAgainst = -1.0;
                        }

                        if (hitRatio != null) {
                            hitRatioRatio = hitRatio[1];
                            hitRatioRatioAgainst = Math.max(hitRatioAverages[1], 1.0);
                            hitRatioMaxRatioAgainst = Math.max(hitRatioAverages[2], 1.5);
                            points += calculatePoints(hitRatioKey, hitRatioRatio, hitRatioRatioAgainst, hitRatioMaxRatioAgainst);
                        } else {
                            hitRatioRatio = -1.0;
                            hitRatioRatioAgainst = -1.0;
                            hitRatioMaxRatioAgainst = -1.0;
                        }

                        if (duration != null) {
                            durationRatio = duration[1];
                            durationRatioAgainst = Math.max(durationAverages[1], 1.0);
                            durationMaxRatioAgainst = Math.max(durationAverages[2], 1.0);
                            points += calculatePointsReverse(PlayerFight.durationKey, durationRatio, durationRatioAgainst, durationMaxRatioAgainst);
                        } else {
                            durationRatio = -1.0;
                            durationRatioAgainst = -1.0;
                            durationMaxRatioAgainst = -1.0;
                        }

                        if (pitchRate != null) {
                            pitchRateRatio = pitchRate[1];
                            pitchRateRatioAgainst = Math.max(pitchRateAverages[1], 1.0);
                            pitchRateMaxRatioAgainst = Math.max(pitchRateAverages[2], 2.0);
                            points += calculatePoints(PlayerFight.pitchRateKey, pitchRateRatio, pitchRateRatioAgainst, pitchRateMaxRatioAgainst);
                        } else {
                            pitchRateRatio = -1.0;
                            pitchRateRatioAgainst = -1.0;
                            pitchRateMaxRatioAgainst = -1.0;
                        }

                        if (yawRate != null) {
                            yawRateRatio = yawRate[1];
                            yawRateRatioAgainst = Math.max(yawRateAverages[1], 4.0);
                            yawRateMaxRatioAgainst = Math.max(yawRateAverages[2], 8.0);
                            points += calculatePoints(PlayerFight.yawRateKey, yawRateRatio, yawRateRatioAgainst, yawRateMaxRatioAgainst);
                        } else {
                            yawRateRatio = -1.0;
                            yawRateRatioAgainst = -1.0;
                            yawRateMaxRatioAgainst = -1.0;
                        }

                        if (cps != null) {
                            cpsRatio = cps[1];
                            cpsRatioAgainst = Math.max(cpsAverages[1], 6.0);
                            cpsMaxRatioAgainst = Math.max(cpsAverages[2], CombatUtils.competitiveCPS);
                            points += calculatePoints(PlayerFight.maxCpsKey, cpsRatio, cpsRatioAgainst, cpsMaxRatioAgainst);
                        } else {
                            cpsRatio = -1.0;
                            cpsRatioAgainst = -1.0;
                            cpsMaxRatioAgainst = -1.0;
                        }

                        if (points >= requiredScore) {
                            killAura.violateAll("type: analysis"
                                    + ", percentage: " + percentage
                                    + ", points: " + points
                                    + ", required-points: " + requiredScore
                                    + ", a: " + AlgebraUtils.cut(winsToLossesRatio, decimalPoint)
                                    + ", b: " + AlgebraUtils.cut(winToLossesRatioAgainst, decimalPoint)
                                    + ", c: " + AlgebraUtils.cut(winToLossesMaxRatioAgainst, decimalPoint)
                                    + ", d: " + AlgebraUtils.cut(reachRatio, decimalPoint)
                                    + ", e: " + AlgebraUtils.cut(reachRatioAgainst, decimalPoint)
                                    + ", f: " + AlgebraUtils.cut(reachMaxRatioAgainst, decimalPoint)
                                    + ", i: " + AlgebraUtils.cut(hitTimeRatio, decimalPoint)
                                    + ", j: " + AlgebraUtils.cut(hitTimeRatioAgainst, decimalPoint)
                                    + ", k: " + AlgebraUtils.cut(hitTimeMaxRatioAgainst, decimalPoint)
                                    + ", l: " + AlgebraUtils.cut(hitComboRatio, decimalPoint)
                                    + ", m: " + AlgebraUtils.cut(hitComboRatioAgainst, decimalPoint)
                                    + ", n: " + AlgebraUtils.cut(hitComboMaxRatioAgainst, decimalPoint)
                                    + ", o: " + AlgebraUtils.cut(hitRatioRatio, decimalPoint)
                                    + ", p: " + AlgebraUtils.cut(hitRatioRatioAgainst, decimalPoint)
                                    + ", q: " + AlgebraUtils.cut(hitRatioMaxRatioAgainst, decimalPoint)
                                    + ", r: " + AlgebraUtils.cut(durationRatio, decimalPoint)
                                    + ", s: " + AlgebraUtils.cut(durationRatioAgainst, decimalPoint)
                                    + ", t: " + AlgebraUtils.cut(durationMaxRatioAgainst, decimalPoint)
                                    + ", v: " + AlgebraUtils.cut(pitchRateRatio, decimalPoint)
                                    + ", u: " + AlgebraUtils.cut(pitchRateRatioAgainst, decimalPoint)
                                    + ", w: " + AlgebraUtils.cut(pitchRateMaxRatioAgainst, decimalPoint)
                                    + ", x: " + AlgebraUtils.cut(yawRateRatio, decimalPoint)
                                    + ", y: " + AlgebraUtils.cut(yawRateRatioAgainst, decimalPoint)
                                    + ", z: " + AlgebraUtils.cut(yawRateMaxRatioAgainst, decimalPoint)
                                    + ", aa: " + AlgebraUtils.cut(cpsRatio, decimalPoint)
                                    + ", ab: " + AlgebraUtils.cut(cpsRatioAgainst, decimalPoint)
                                    + ", ac: " + AlgebraUtils.cut(cpsMaxRatioAgainst, decimalPoint));
                        }
                    }
                }
            }
        }
    }
}
