package com.vagdedes.spartan.checks.combat.killAura;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.abstraction.DetectionExecutor;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.data.Decimals;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.entity.LivingEntity;

// Study of the speed difference when combating in regard to consistency

public class Movement extends DetectionExecutor {

    Movement(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    private static final String key = Movement.class.getName();
    private static final double
            distanceRequirement = 2.5,
            differenceRequirement = 0.01;

    void run(LivingEntity entity, double distance, double width) {
        double entityWidth = CombatUtils.getWidthAndHeight(entity)[0],
                distanceRequirementModified = Math.max(distanceRequirement, entityWidth);

        if (distance > distanceRequirementModified // We want the player at a distance, otherwise we risk the width being within the limits
                && (Math.abs(width) <= entityWidth || player.getProfile().isSuspectedOrHacker(hackType))) {
            Double nmsDistance = player.getNmsDistance();

            if (nmsDistance != null
                    && nmsDistance >= KillAura.universalNmsDistanceRequirement) {
                Decimals decimals = player.getDecimals();
                double oldNmsDistance = decimals.get(key);

                if (decimals.has(oldNmsDistance)) {
                    double difference = Math.abs(nmsDistance - oldNmsDistance);
                    Buffer buffer = player.getBuffer(hackType);

                    if (difference < differenceRequirement) {
                        if (buffer.start(key, 10 * CombatUtils.getFightTicks(player)) == 6) {
                            buffer.remove(key);
                            cancel("type: movement"
                                    + ", width: " + width
                                    + ", entity-width: " + entityWidth
                                    + ", diagonal-distance: " + distance
                                    + ", distance-requirement: " + distanceRequirementModified
                                    + ", nms-distance: " + nmsDistance
                                    + ", old-nms-distance: " + oldNmsDistance
                                    + ", nms-distance-requirement: " + KillAura.universalNmsDistanceRequirement
                                    + ", difference: " + difference
                                    + ", difference-requirement: " + differenceRequirement);
                        }
                    } else {
                        buffer.decrease(key, 1);
                    }
                }
                decimals.set(key, nmsDistance);
            } else {
                player.getBuffer(hackType).decrease(key, 1);
            }
        } else {
            player.getBuffer(hackType).decrease(key, 1);
        }
    }
}
