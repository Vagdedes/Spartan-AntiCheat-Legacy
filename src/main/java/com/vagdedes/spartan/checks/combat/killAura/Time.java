package com.vagdedes.spartan.checks.combat.killAura;

// Study of amount of entities hit in a given time

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.CombatProcessing;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Time extends DetectionExecutor {

    Time(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    private static final String key = Time.class.getName() + "=";
    private static final int[][] criteria = new int[][]{ // Always smallest to the biggest number/s order
            new int[]{1, 2}, // 0.5
            new int[]{5, 3}, // 1.66
            new int[]{10, 5}, // 2
            new int[]{20, 8}, // 2.5
            new int[]{40, 13}, // 3.07
    };
    private static long dynamicMinimumTime = CombatUtils.defaultFightTicks * 50L;

    void run(LivingEntity entity, double width, double distance) {
        boolean first = false;

        for (int[] ints : criteria) {
            if (!first) {
                first = true;

                if (NoHitDelay.hasCooldown(player)) {
                    continue;
                } else if (entity instanceof Player) {
                    SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                    if (target != null && NoHitDelay.hasCooldown(target)) {
                        continue;
                    }
                }
            }
            int ticks = ints[0], entities = ints[1];
            String key = flag(entity, ticks, entities);

            if (key != null) {
                if (!key.isEmpty()) { // If the length is zero, it means the required amount of nearby entities was not met, thus it's pointless to continue the loop
                    player.getBuffer(hackType).clear(key); // Clear whatever data is potentially remaining with this semi-heavy method
                    cancel("type: time(speed), ticks: " + ticks + ", entities: " + entities);
                }
                break;
            }
        }

        // Separator

        if (player.movement.isMoving(true)) {
            double entityWidth = CombatUtils.getWidthAndHeight(entity)[0];

            if (distance > entityWidth // We want the player in a certain distance, otherwise we risk having easily low width
                    && Math.abs(width) <= entityWidth) {
                int cps = player.getClicks().getCount();

                if (cps >= 8) {
                    double yawRate = CombatProcessing.getDecimal(player, CombatProcessing.yawDifference, 0.0),
                            pitchRate = CombatProcessing.getDecimal(player, CombatProcessing.pitchDifference, 0.0);

                    if (yawRate > 0.0 && pitchRate > 0.0) {
                        Timer timer = player.getTimer();
                        String subKey = key + "=" + entity.getEntityId();
                        long time = timer.get(subKey);
                        timer.set(subKey);

                        if (time <= dynamicMinimumTime) {
                            dynamicMinimumTime = Math.max(time, 50L);
                            Buffer buffer = player.getBuffer(hackType);
                            int ticks = 10 * ((int) dynamicMinimumTime);

                            if (buffer.start(subKey, ticks, Check.violationsMeasurementTicks) == (8 / (double) ticks)) {
                                buffer.remove(subKey);
                                cancel("type: time(consistency)"
                                        + ", width: " + width
                                        + ", entity-width: " + entityWidth
                                        + ", yaw-rate: " + yawRate
                                        + ", pitch-rate: " + pitchRate
                                        + ", time: " + time
                                        + ", time-requirement: " + dynamicMinimumTime
                                        + ", clicks-per-second" + cps);
                            }
                        }
                    }
                }
            }
        }
    }

    private String flag(LivingEntity entity, int ticks, int entityRequirement) {
        List<Entity> nearbyEntities = player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);
        int nearbyEntitiesCount = nearbyEntities.size();

        if (nearbyEntitiesCount > 0) {
            String subKey = key + ticks + "=" + entityRequirement,
                    entityKey = subKey + "=" + entity.getEntityId();
            Buffer buffer = player.getBuffer(hackType); // Do not use the combat buffer as the passing of time is very important
            int remainingTicks = buffer.getRemainingTicks(subKey, ticks);
            List<Integer> validEntities = new ArrayList<>();

            if (remainingTicks == 0) { // Start of the buffer
                for (Entity nearbyEntity : nearbyEntities) {
                    if (nearbyEntity instanceof LivingEntity) {
                        int id = nearbyEntity.getEntityId();
                        validEntities.add(id);
                        buffer.remove(subKey + "=" + id);
                    }
                }
                buffer.remove(entityKey);
                remainingTicks = ticks;
            } else {
                for (Entity nearbyEntity : nearbyEntities) {
                    if (nearbyEntity instanceof LivingEntity) {
                        int id = nearbyEntity.getEntityId();
                        validEntities.add(id);
                    }
                }
            }

            if (validEntities.size() == entityRequirement) {
                if (buffer.count(entityKey, remainingTicks) == 1 // Entity data always before detection buffer
                        && buffer.count(subKey, ticks) == entityRequirement) {
                    for (int id : validEntities) {
                        buffer.remove(subKey + "=" + id);
                    }
                    buffer.remove(entityKey);
                    buffer.remove(subKey);
                    return subKey;
                }
            } else {
                return "";
            }
        } else {
            return "";
        }
        return null;
    }
}
