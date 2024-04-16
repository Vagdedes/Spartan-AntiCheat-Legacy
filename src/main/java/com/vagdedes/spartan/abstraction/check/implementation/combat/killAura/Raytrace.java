package com.vagdedes.spartan.abstraction.check.implementation.combat.killAura;

// Study of hitting entities behind other entities or blocks

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;

public class Raytrace extends DetectionExecutor {

    Raytrace(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    private final KillAura killAura = (KillAura) executor;

    private static final double
            multiplication = 0.1, // 1/10th of a block at a time is enough to cover all positions while being performance friendly
            closeness = 0.075;
    private static final int maxEntities = 5; // 5 because after that there are far too many to safely check for collisions

    void run(SpartanLocation location, LivingEntity entity, double distance) {
        if (!player.bedrockPlayer) {
            double entityWidth = CombatUtils.getWidthAndHeight(entity)[0];

            if (distance > entityWidth) {
                List<Entity> nearbyEntities = player.getNearbyEntities(distance, distance, distance);
                nearbyEntities.remove(entity);
                int nearbyEntitiesSize = nearbyEntities.size();

                if (nearbyEntitiesSize <= maxEntities) {
                    PlayerProfile playerProfile = player.getProfile();
                    Handlers handlers = player.getHandlers();
                    boolean hasNearbyEntities = nearbyEntitiesSize > 0, fullSolid = false;

                    SpartanLocation playerEyeHeightLocation = location.clone().add(0.0, player.getEyeHeight(), 0.0);
                    Vector vector = playerEyeHeightLocation.getDirection();
                    SpartanBlock block = playerEyeHeightLocation.getBlock();

                    Location entityLocation = entity.getLocation();
                    double entityLocationX = entityLocation.getBlockX(),
                            entityLocationZ = entityLocation.getBlockZ();

                    for (double raytrace = (entityWidth + multiplication); raytrace < (Math.floor(distance / multiplication) * multiplication); raytrace += multiplication) {
                        SpartanLocation raytracedLocation = playerEyeHeightLocation.clone().add(
                                new Vector(vector.getX(), vector.getY(), vector.getZ()).multiply(raytrace)
                        );
                        double raytracedLocationX = raytracedLocation.getBlockX(),
                                raytracedLocationZ = raytracedLocation.getBlockZ();

                        if (raytracedLocationX != entityLocationX || raytracedLocationZ != entityLocationZ) { // Do not check the x & z coordinates of the entity
                            double raytracedLocationY = raytracedLocation.getBlockY(), distanceToBlock;

                            if (raytracedLocationX != block.x
                                    || raytracedLocationY != block.y
                                    || raytracedLocationZ != block.z) {
                                block = raytracedLocation.getBlock();
                                distanceToBlock = raytracedLocation.distance(block.getLocation());
                                fullSolid = (!handlers.has(Handlers.HandlerType.SensitiveBlockBreak) || !BlockUtils.isSensitive(player, block.material))
                                        && BlockUtils.isFullSolid(block.material);
                                //(playerProfile.isSuspectedOrHacker(hackType) || distanceToBlock <= 1.0) Attention: recently removed
                            } else {
                                distanceToBlock = 0.0;
                            }

                            if (fullSolid) {
                                cancel(
                                        "type: raytrace(block)"
                                                + ", raytrace: " + raytrace
                                                + ", diagonal-distance: " + distance
                                                + ", distance-to-block: " + distanceToBlock
                                                + ", entity-to-block-distance: " + block.getLocation().distance(entityLocation)
                                                + ", block: " + BlockUtils.blockToString(block)
                                                + ", damaged-entity: " + CombatUtils.entityToString(entity)
                                                + ", nearby-entities: " + nearbyEntitiesSize);
                                return;
                            } else if (hasNearbyEntities) {
                                for (Entity raytracedEntity : nearbyEntities) {
                                    if (raytracedEntity instanceof LivingEntity
                                            && killAura.canRunEntity((LivingEntity) raytracedEntity)) {
                                        Location raytracedEntityLocation = raytracedEntity.getLocation();
                                        double entityHorizontalDistanceToRaytracedLocation = AlgebraUtils.getHorizontalDistance(raytracedEntityLocation, raytracedLocation);

                                        if (entityHorizontalDistanceToRaytracedLocation <= closeness) {
                                            double entityY = raytracedEntityLocation.getY(),
                                                    entityHeightY = entityY + CombatUtils.getWidthAndHeight((LivingEntity) raytracedEntity)[1];

                                            if (raytracedLocationY >= entityY && raytracedLocationY <= entityHeightY) { // Y coordinate must be within the entity's height
                                                cancel(
                                                        "type: raytrace(entity)"
                                                                + ", raytrace: " + raytrace
                                                                + ", diagonal-distance: " + distance
                                                                + ", blocking-entity: " + CombatUtils.entityToString(raytracedEntity)
                                                                + ", damaged-entity: " + CombatUtils.entityToString(entity)
                                                                + ", location-y: " + raytracedLocationY
                                                                + ", entity-min-y: " + entityY
                                                                + ", entity-max-y: " + entityHeightY
                                                                + ", entity-distance: " + entityHorizontalDistanceToRaytracedLocation
                                                                + ", nearby-entities: " + nearbyEntitiesSize);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            break; // There's no need to continue the loop when the entity's x & z coordinates have been reached
                        }
                    }
                }
            }
        }
    }
}
