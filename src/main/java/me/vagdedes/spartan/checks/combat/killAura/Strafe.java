package me.vagdedes.spartan.checks.combat.killAura;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Strafe {

    private static final Map<UUID, Vector> directions = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
    private static final String
            key = Strafe.class.getName(),
            nmsKey = key + "=nms";
    private static final double
            differenceRequirement = 0.001;

    static void remove(SpartanPlayer player) {
        directions.remove(player.getUniqueId());
    }

    static void run(SpartanPlayer player, SpartanLocation location, Buffer buffer) {
        if (player.isMoving(true) && Damage.getLastDealt(player) <= KillAura.pastDamageMilliseconds) {
            List<Entity> entities = player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

            if (entities.size() > 0) {
                for (Entity nearbyEntity : entities) {
                    if (nearbyEntity instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity) nearbyEntity;

                        if (KillAura.canRunEntity(player, entity)) {
                            double[] utility = CombatUtils.get_X_Y_Distance(player, entity);

                            if (utility != null) {
                                double width = utility[0],
                                        distance = utility[2],
                                        entityWidth = CombatUtils.getWidthAndHeight(entity)[0];

                                if (distance > entityWidth // We want the player in a certain distance, otherwise we risk having easily low width
                                        && Math.abs(width) <= Math.min(entityWidth + 0.1, KillAura.normalWidthToPlayerView)) {
                                    UUID uuid = player.getUniqueId();
                                    Vector direction = location.getDirection(),
                                            oldDirection = directions.get(uuid);

                                    if (oldDirection != null) {
                                        Decimals decimals = player.getDecimals(KillAura.check);
                                        double directionalDistance = direction.distance(oldDirection),
                                                oldDirectionalDistance = decimals.get(key, 0.0),
                                                nmsDistance = player.getNmsDistance(),
                                                oldNmsDistance = decimals.get(nmsKey, 0.0);

                                        if (directionalDistance > 0.0
                                                && oldDirectionalDistance > 0.0
                                                && nmsDistance >= KillAura.universalNmsDistanceRequirement
                                                && oldNmsDistance >= KillAura.universalNmsDistanceRequirement) {
                                            double distanceDifference = Math.abs(directionalDistance - oldDirectionalDistance),
                                                    nmsDistanceDifference = Math.abs(nmsDistance - oldNmsDistance);

                                            if (distanceDifference < differenceRequirement
                                                    && nmsDistanceDifference < differenceRequirement
                                                    && buffer.start(key, 20) == 7) {
                                                KillAura.violate(player, "type: strafe"
                                                        + ", directional-distance: " + directionalDistance
                                                        + ", directional-distance-difference: " + distanceDifference
                                                        + ", nms-distance: " + nmsDistance
                                                        + ", nms-distance-difference: " + nmsDistanceDifference
                                                        + ", nms-distance-requirement: " + KillAura.universalNmsDistanceRequirement
                                                        + ", difference-requirement: " + differenceRequirement
                                                        + ", diagonal-distance: " + distance
                                                        + ", width: " + width
                                                        + ", entity-width: " + entityWidth);
                                            }
                                        }
                                        decimals.set(key, directionalDistance);
                                        decimals.set(nmsKey, nmsDistance);
                                    }
                                    directions.put(uuid, direction);
                                    break; // We just need one entity in angle to run this detection, so we break the loop
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
