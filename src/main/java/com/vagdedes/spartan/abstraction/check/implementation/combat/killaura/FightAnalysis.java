package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.event.EntityAttackPlayerEvent;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.utils.java.OverflowMap;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.statistics.StatisticsMath;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FightAnalysis extends ProbabilityDetection {

    private static final Map<UUID, Map<Integer, int[]>> data = new OverflowMap<>(
            new ConcurrentHashMap<>(2),
            2_048
    );
    private static final double desiredConfidence = 0.9;
    private static final int desiredCount = AlgebraUtils.integerCeil(
            1.0 / (1.0 - desiredConfidence)
    );

    FightAnalysis(CheckRunner executor) {
        super(executor, null, null, "fight_analysis", true);
    }

    void runDealDamage(PlayerAttackEvent e) {
        this.call(() -> {
            LivingEntity target = e.target;
            int identifier;

            if (target instanceof Player) {
                identifier = target.getName().hashCode();
            } else {
                identifier = target.getType().toString().hashCode();
            }
            Map<Integer, int[]> subData = data.computeIfAbsent(
                    this.protocol.getUUID(),
                    k -> new ConcurrentHashMap<>(2)
            );
            int[] hitsDamages = subData.getOrDefault(identifier, new int[2]);
            hitsDamages[0]++;
            subData.put(identifier, hitsDamages);
            double ratio = hitsDamages[0] / (double) hitsDamages[1],
                    probability = probability(identifier, ratio);

            if (probability >= desiredConfidence) {
                cancel(
                        "type: fight-analysis, ratio: " + ratio + ", probability: " + probability
                );
            }
        });
    }

    void runReceiveDamage(EntityAttackPlayerEvent e) {
        this.call(() -> {
            LivingEntity damager = e.damager;
            int identifier;

            if (damager instanceof Player) {
                identifier = damager.getName().hashCode();
            } else {
                identifier = damager.getType().toString().hashCode();
            }
            Map<Integer, int[]> subData = data.computeIfAbsent(
                    this.protocol.getUUID(),
                    k -> new OverflowMap<>(
                            new ConcurrentHashMap<>(2),
                            EntityType.values().length
                    )
            );
            int[] hitsDamages = subData.getOrDefault(identifier, new int[2]);
            hitsDamages[1]++;
            subData.put(identifier, hitsDamages);
        });
    }

    private double probability(int identifier, double selfRatio) {
        double sum = 0.0,
                variance = 0.0;
        int count = 0;

        for (Map<Integer, int[]> subData : data.values()) {
            int[] hitsDamages = subData.get(identifier);

            if (hitsDamages != null) {
                double ratio = hitsDamages[0] / (double) hitsDamages[1];
                sum += ratio;
                count++;
            }
        }
        if (count < desiredCount) {
            return -1.0;
        }
        sum /= count;

        for (Map<Integer, int[]> subData : data.values()) {
            int[] hitsDamages = subData.get(identifier);

            if (hitsDamages != null) {
                double ratio = hitsDamages[0] / (double) hitsDamages[1];
                variance += Math.pow(ratio - sum, 2);
            }
        }
        double squareSum = Math.sqrt(variance / count);
        return StatisticsMath.getCumulativeProbability(
                (sum - selfRatio) / squareSum
        );
    }

}
