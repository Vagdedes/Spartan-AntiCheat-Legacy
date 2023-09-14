package me.vagdedes.spartan.checks.combat.killAura;

// Study of the head movement from before to the moment of hitting an entity, but also the activity of the direction

import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;

public class Rotations {

    private static final String key = Rotations.class.getName() + "=";
    private static final int requiredStoredValues = 5;

    static void run(SpartanPlayer player, LivingEntity entity, double width) {
        if (!CombatUtils.hasBlockBehind(player, entity)) {
            List<Double> yawList = CombatProcessing.getAllValues(player, CombatProcessing.yawDifference);
            int yawListSize = yawList.size();

            if (yawListSize >= requiredStoredValues) {
                double entityWidth = CombatUtils.getWidthAndHeight(entity)[0],
                        widthAccuracy = player.getProfile().isSuspectedOrHacker(KillAura.check) ? -1.0 : CombatUtils.getWidthAccuracyPercentage(width, entityWidth);

                if (widthAccuracy != 0.0) { // Only check when accuracy is within the entity's width (Off-width is checked by 'Direction')
                    Vector yawDirection = CombatUtils.getDirection(player.getLocation().getYaw(), 0.0f);
                    double averageYawDistance = 0.0,
                            maxYawDistance = 0.0;
                    int skip = yawListSize - requiredStoredValues,
                            validPositions = 0;

                    for (double yaw : yawList) {
                        if (skip == 0) {
                            validPositions++;
                            Vector loopDirection = CombatUtils.getDirection((float) yaw, 0.0f);
                            double directionalDistance = loopDirection.distance(yawDirection);
                            averageYawDistance += directionalDistance;
                            maxYawDistance = Math.max(directionalDistance, maxYawDistance);
                        } else {
                            skip--;
                        }
                    }

                    if (validPositions >= 2) { // We need at least 2 results to be compared
                        averageYawDistance /= validPositions;
                        double extraWidth = Math.max(entityWidth - KillAura.normalWidthToPlayerView, 0.0), // Most entities that were used to test this detection hovered around this number
                                distanceRatio = (maxYawDistance / averageYawDistance) - extraWidth;

                        if (distanceRatio >= 4.0) {
                            KillAura.violate(player,
                                    "type: rotations(yaw-max)"
                                            + ", width: " + width
                                            + ", entity-width: " + entityWidth
                                            + ", width-accuracy: " + widthAccuracy
                                            + ", max-distance: " + maxYawDistance
                                            + ", average-distance: " + averageYawDistance
                                            + ", distance-ratio: " + distanceRatio
                                            + ", extra-width: " + extraWidth
                                            + ", entity: " + CombatUtils.entityToString(entity)
                                            + ", required-positions: " + requiredStoredValues
                                            + ", valid-positions: " + validPositions);
                        } else if (distanceRatio >= 2.0) {
                            Buffer buffer = player.getBuffer(KillAura.check);

                            if (buffer.start(Buffer.BufferType.Combat, key, CombatUtils.getFightTicks(player) * 5) == 2) {
                                buffer.remove(key);
                                KillAura.violate(player,
                                        "type: rotations(yaw-buffered)"
                                                + ", width: " + width
                                                + ", entity-width: " + entityWidth
                                                + ", width-accuracy: " + widthAccuracy
                                                + ", max-distance: " + maxYawDistance
                                                + ", average-distance: " + averageYawDistance
                                                + ", distance-ratio: " + distanceRatio
                                                + ", extra-width: " + extraWidth
                                                + ", entity: " + CombatUtils.entityToString(entity)
                                                + ", required-positions: " + requiredStoredValues
                                                + ", valid-positions: " + validPositions);
                            }
                        }
                    }
                }
            }
        }
    }
}
