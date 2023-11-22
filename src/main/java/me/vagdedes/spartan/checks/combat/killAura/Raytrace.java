package me.vagdedes.spartan.checks.combat.killAura;

// Study of hitting entities behind other entities or blocks

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.abstraction.DetectionExecutor;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Location;
import org.bukkit.Material;
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
        if (!player.isBedrockPlayer()) {
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

                            if (raytracedLocationX != block.getX()
                                    || raytracedLocationY != block.getY()
                                    || raytracedLocationZ != block.getZ()) {
                                block = raytracedLocation.getBlock();
                                distanceToBlock = raytracedLocation.distance(block.getLocation());
                                Material type = block.getType();
                                fullSolid = (!handlers.has(Handlers.HandlerType.SensitiveBlockBreak) || !BlockUtils.isSensitive(player, type))
                                        && BlockUtils.isFullSolid(type);
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
