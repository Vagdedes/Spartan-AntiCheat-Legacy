package com.vagdedes.spartan.checks.combat.killAura;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;

public class Strafe extends DetectionExecutor {

    private final KillAura killAura;
    private Vector oldDirection;

    Strafe(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        killAura = (KillAura) executor;
        oldDirection = executor.player.movement.getLocation().getDirection();
    }

    private static final String
            key = Strafe.class.getName(),
            nmsKey = key + "=nms";
    private static final double differenceRequirement = 0.001;

    void run(SpartanLocation location, Buffer buffer) {
        if (player.movement.isMoving(true)
                && player.getLastDamageDealt().ticksPassed() <= KillAura.pastDamageMilliseconds) {
            List<Entity> entities = player.getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

            if (!entities.isEmpty()) {
                for (Entity nearbyEntity : entities) {
                    if (nearbyEntity instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity) nearbyEntity;

                        if (killAura.canRunEntity(entity)) {
                            double[] utility = CombatUtils.get_X_Y_Distance(player, entity);

                            if (utility != null) {
                                double width = utility[0],
                                        distance = utility[2],
                                        entityWidth = CombatUtils.getWidthAndHeight(entity)[0];

                                if (distance > entityWidth // We want the player in a certain distance, otherwise we risk having easily low width
                                        && Math.abs(width) <= Math.min(entityWidth + 0.1, KillAura.normalWidthToPlayerView)) {
                                    Vector direction = location.getDirection();
                                    Decimals decimals = player.getDecimals(hackType);
                                    Double nmsDistance = player.movement.getNmsDistance();
                                    double directionalDistance = direction.distance(oldDirection),
                                            oldDirectionalDistance = decimals.get(key, 0.0),
                                            oldNmsDistance = decimals.get(nmsKey, 0.0);

                                    if (directionalDistance > 0.0
                                            && oldDirectionalDistance > 0.0
                                            && nmsDistance != null && nmsDistance >= KillAura.universalNmsDistanceRequirement
                                            && oldNmsDistance >= KillAura.universalNmsDistanceRequirement) {
                                        double distanceDifference = Math.abs(directionalDistance - oldDirectionalDistance),
                                                nmsDistanceDifference = Math.abs(nmsDistance - oldNmsDistance);

                                        if (distanceDifference < differenceRequirement
                                                && nmsDistanceDifference < differenceRequirement
                                                && buffer.start(key, 20, Check.violationsMeasurementTicks) >= 0.35) {
                                            cancel("type: strafe"
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

                                    if (nmsDistance != null) {
                                        decimals.set(nmsKey, nmsDistance);
                                    } else {
                                        decimals.remove(nmsKey);
                                    }
                                    oldDirection = direction;
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
