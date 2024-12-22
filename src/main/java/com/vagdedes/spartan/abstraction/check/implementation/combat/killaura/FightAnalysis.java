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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class FightAnalysis extends ProbabilityDetection {

    private static final Map<UUID, Map<Integer, int[]>> data = new OverflowMap<>(
            new LinkedHashMap<>(2),
            2_048
    );
    private static final double desiredConfidence = 0.9;
    private static final int desiredCount = AlgebraUtils.integerCeil(
            1.0 / (1.0 - desiredConfidence)
    );

    FightAnalysis(CheckRunner executor) {
        super(executor, "fight_analysis", true);
    }

    final void runDealDamage(PlayerAttackEvent e) {
        LivingEntity target = e.target;
        int identifier;

        if (target instanceof Player) {
            identifier = target.getName().hashCode();
        } else {
            identifier = target.getType().toString().hashCode();
        }
        Map<Integer, int[]> subData = data.computeIfAbsent(
                this.protocol().getUUID(),
                k -> new LinkedHashMap<>(2)
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
    }

    final void runReceiveDamage(EntityAttackPlayerEvent e) {
        LivingEntity damager = e.damager;
        int identifier;

        if (damager instanceof Player) {
            identifier = damager.getName().hashCode();
        } else {
            identifier = damager.getType().toString().hashCode();
        }
        Map<Integer, int[]> subData = data.computeIfAbsent(
                this.protocol().getUUID(),
                k -> new OverflowMap<>(
                        new LinkedHashMap<>(2),
                        EntityType.values().length
                )
        );
        int[] hitsDamages = subData.getOrDefault(identifier, new int[2]);
        hitsDamages[1]++;
        subData.put(identifier, hitsDamages);
    }

    private double probability(int identifier, double selfRatio) {
        double sum = 0.0,
                squareSum = 0.0;
        int count = 0;

        for (Map<Integer, int[]> subData : data.values()) {
            int[] hitsDamages = subData.get(identifier);

            if (hitsDamages != null) {
                double ratio = hitsDamages[0] / (double) hitsDamages[1];
                sum += ratio;
                squareSum += ratio * ratio;
                count++;
            }
        }
        if (count < desiredCount) {
            return -1.0;
        }
        sum /= count;
        squareSum = Math.sqrt(squareSum / count);
        return StatisticsMath.getCumulativeProbability(
                (sum - selfRatio) / squareSum
        );
    }

}
