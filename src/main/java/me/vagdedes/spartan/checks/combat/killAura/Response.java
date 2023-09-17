package me.vagdedes.spartan.checks.combat.killAura;

// Study of how many ticks the player had a close angle on an entity before hitting it

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class Response extends DetectionExecutor {

    private final KillAura killAura;

    Response(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        killAura = (KillAura) executor;
    }

    private static final String key = Response.class.getName();
    private static final int
            bufferTicksMemory = 20,
            bufferFails = 3,
            maxEntities = 3; // 3 because we don't want the player turning around and having a high chance of hitting entities

    void run(LivingEntity entity, double width, double distance) {
        double entityWidth = CombatUtils.getWidthAndHeight(entity)[0];

        if (distance > entityWidth // We want the player in a certain distance, otherwise we risk having easily low width, also this is the opposite of the loop
                && Math.abs(width) <= (Math.min(entityWidth, KillAura.normalWidthToPlayerView) / (player.getProfile().isSuspectedOrHacker(hackType) ? 1.0 : 2.0)) // The width must be appropriate, otherwise it's the purpose of another detection to cover this
                && player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance).size() <= maxEntities) {
            Buffer buffer = player.getBuffer(hackType);
            String key = Response.key + "=" + entity.getEntityId();
            int angleTicks = buffer.get(key);
            buffer.set(key, angleTicks + 1);

            if (angleTicks == 0) {
                int bufferTicks = CombatUtils.getFightTicks(player) * 5;

                if (buffer.start(Buffer.BufferType.Combat, key + "=run", bufferTicks) == 1
                        && buffer.start(Buffer.BufferType.Combat, Response.key, bufferTicks) == 2) {
                    buffer.clear(Response.key);
                    cancel("type: response"
                            + ", width: " + width
                            + ", entity-width: " + entityWidth
                            + ", diagonal-distance: " + distance
                            + ", entity: " + CombatUtils.entityToString(entity));
                }
            }
        }
    }

    void runMove(Buffer buffer) {
        if (buffer.start(key + "=tick", 1) == Math.max(LagLeniencies.getDelay(player), 1)) { // This algorithm runs regardless of combat, so makes sure it runs once per tick or one's latency
            List<Entity> entities = player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

            if (!entities.isEmpty()) {
                for (Entity nearbyEntity : entities) {
                    if (nearbyEntity instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity) nearbyEntity;
                        String key = Response.key + "=" + entity.getEntityId();

                        if (killAura.canRunEntity(entity)) {
                            double[] utility = CombatUtils.get_X_Y_Distance(player, entity);

                            if (utility != null) {
                                String removeKey = key + "=remove";

                                if (Math.abs(utility[0]) <= (CombatUtils.getWidthAndHeight(entity)[0] * 1.5)) {
                                    buffer.remove(removeKey);
                                    buffer.start(key, bufferTicksMemory);
                                    buffer.setRemainingTicks(key, bufferTicksMemory);
                                } else if (buffer.start(removeKey, bufferTicksMemory) >= bufferFails) {
                                    buffer.remove(removeKey);
                                    buffer.remove(key);
                                }
                            } else {
                                buffer.remove(key);
                            }
                        } else {
                            buffer.remove(key);
                        }
                    }
                }
            }
        }
    }
}
