package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.*;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public class MovementProcessing {

    private static final boolean
            v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8),
            v1_9 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9);
    private static final int roundedFall = AlgebraUtils.integerCeil(PlayerUtils.terminalVelocity);
    private static final Material MAGMA_BLOCK = MaterialUtils.get("magma");

    public static void run(Player n, SpartanPlayer player,
                           SpartanLocation to,
                           SpartanLocation from,
                           double distance, double horizontal,
                           double vertical, double box,
                           float fall) {
        player.movement.processLastMoveEvent(to, from);

        // Separator

        if (player.refreshOnGroundCustom(to)) {
            player.movement.setAirTicks(0);
            player.movement.setFallingTicks(0);
        } else {
            Double old_NmsVerticalDistance = player.movement.getPreviousNmsVerticalDistance();

            if (old_NmsVerticalDistance != null
                    && vertical < old_NmsVerticalDistance) {
                player.movement.setFallingTicks(player.movement.getFallingTicks() + 1);
            } else {
                player.movement.setFallingTicks(0);
            }
            player.movement.setAirTicks(player.movement.getTicksOnAir() + 1);
        }

        // Separator

        if (player.isOnGround()) {
            player.movement.setVanillaAirTicks(0);
            player.movement.setVanillaFallingTicks(0);
        } else {
            Double old_NmsVerticalDistance = player.movement.getPreviousNmsVerticalDistance();

            if (old_NmsVerticalDistance != null
                    && vertical < old_NmsVerticalDistance) {
                player.movement.setVanillaFallingTicks(player.movement.getFallingTicks() + 1);
            } else {
                player.movement.setVanillaFallingTicks(0);
            }
            player.movement.setVanillaAirTicks(player.movement.getTicksOnAir() + 1);
        }

        // Separator

        if (to.getYaw() != from.getYaw() || to.getPitch() != from.getPitch()) {
            player.movement.setLastHeadMovement();
        }

        // Separator

        GameMode current = player.getGameMode();

        if (current == GameMode.CREATIVE
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && current == GameMode.SPECTATOR) {
            player.getTrackers().add(Trackers.TrackerType.GameMode, (int) TPS.maximum);
        }

        // Separator

        if (n.isSprinting()) {
            player.movement.setSprinting(true);
            player.movement.setSneaking(false);
        } else {
            player.movement.setSprinting(false);
            player.movement.setSneaking(n.isSneaking());
        }
        player.movement.setNmsDistance(distance, horizontal, vertical, box, fall);
        player.setFallDistance(n.getFallDistance(), false);
        player.setWalkSpeed(n.getWalkSpeed());
        player.setFlySpeed(n.getFlySpeed());
        player.setEyeHeight(n.getEyeHeight());
        player.setUsingItem(n.isBlocking());
        player.setVehicle(n.getVehicle());

        // Separator

        if (!calculateLiquid(player, to)) {
            if (Math.abs(PlayerUtils.jumping[1] - vertical) < PlayerUtils.getJumpingPrecision(player)) { // Last Jump
                player.movement.setLastJump();
            } else if (player.movement.isFalling(vertical)) { // Last Fall
                player.movement.setLastFall();
            }
            if (!calculateGroundCollision(player, vertical, box)) {
                calculateBouncing(player, to, vertical);
            }
        } else {
            calculateBouncing(player, to, vertical);
        }
        calculateExtremeCollision(player, to);

        // Separator

        String key = "player-data=extra-packets";
        double difference = player.movement.getCustomDistance() - distance;
        player.getDecimals().add(key, difference, AlgebraUtils.integerRound(TPS.maximum));

        if (player.getBuffer().increase(key, 1) >= TPS.maximum) {
            player.getBuffer().remove(key);

            if (player.getDecimals().get(key, 0.0, Decimals.CALCULATE_AVERAGE) >= 0.01) {
                player.movement.setExtraPackets(player.movement.getExtraPackets() + 1);
            } else {
                player.movement.setExtraPackets(0);
            }
        }

        // Separator

        ServerFlying.run(player);
    }

    private static boolean calculateGroundCollision(SpartanPlayer player,
                                                    double vertical, double box) {
        if (player.isOnGroundCustom()
                && player.isOnGround()
                && player.movement.getTicksOnAir() == 0
                && player.getVehicle() == null
                && vertical == 0.0
                && GroundUtils.collisionHeightExists(box)) {
            Trackers handlers = player.getTrackers();
            handlers.removeMany(Trackers.TrackerFamily.Motion);
            handlers.removeMany(Trackers.TrackerFamily.Velocity);
            player.movement.removeLastLiquidTime();
            return true;
        } else {
            return false;
        }
    }

    private static void calculateExtremeCollision(SpartanPlayer player, SpartanLocation location) {
        if (v1_9) {
            Trackers handlers = player.getTrackers();

            if (!handlers.isDisabled(Trackers.TrackerType.ExtremeCollision)) {
                handlers.disable(Trackers.TrackerType.ExtremeCollision, 2); // Disable for the next tick
                int collisions = PlayerUtils.getNearbyCollisions(player, location);

                if (collisions > 10) {
                    handlers.add(Trackers.TrackerType.ExtremeCollision, Math.min((int) (TPS.maximum * 2), collisions));
                }
            }
        }
    }

    private static void calculateBouncing(SpartanPlayer player, SpartanLocation location,
                                          double vertical) {
        if (v1_8 && vertical != 0.0) {
            if (BlockUtils.isSlime(player, location, roundedFall)) {
                int time = (int) (TPS.maximum * 2);
                player.getTrackers().add(Trackers.TrackerType.BouncingBlocks, time);
                player.getTrackers().add(Trackers.TrackerType.BouncingBlocks, "slime", time);
            } else if (BlockUtils.isBed(player, location, roundedFall)) {
                int time = (int) (TPS.maximum * 2);
                player.getTrackers().add(Trackers.TrackerType.BouncingBlocks, time);
                player.getTrackers().add(Trackers.TrackerType.BouncingBlocks, "bed", time);
            }
        }
    }

    // Separator

    private static boolean calculateLiquid(SpartanPlayer player, SpartanLocation location) {
        if (player.movement.isSwimming()) {
            player.movement.setLastLiquidTime();
            calculateBubbleWater(player, location);
            return true;
        } else {
            double hitbox = player.bedrockPlayer ? BlockUtils.hitbox_max : BlockUtils.hitbox;

            if (location.getBlock().isLiquidOrWaterLogged()
                    || PatternUtils.isLiquidPattern(new double[][]{
                    {hitbox, 0, hitbox},
                    {hitbox, 1, hitbox},
                    {hitbox, player.getEyeHeight() + PlayerUtils.lowPrecision, hitbox}
            }, location, true)) {
                player.movement.setLastLiquidTime();

                if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                        && Compatibility.CompatibilityType.ViaVersion.isFunctional()
                        && Math.round(player.getEyeHeight() * 10.0) == 4.0) {
                    player.movement.setSwimming(false, 10);
                }
                calculateBubbleWater(player, location);
                return true;
            }
        }
        return false;
    }

    private static void calculateBubbleWater(SpartanPlayer player, SpartanLocation location) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            int blockY = location.getBlockY(), minY = BlockUtils.getMinHeight(player.getWorld());

            if (blockY > minY) {
                SpartanLocation locationModified = location.clone();

                for (int i = 0; i <= (blockY - minY); i++) {
                    int nonLiquid = 0;
                    SpartanLocation[] locations = locationModified.clone().add(0, -i, 0).getSurroundingLocations(BlockUtils.hitbox, 0, BlockUtils.hitbox);

                    for (SpartanLocation loc : locations) {
                        SpartanBlock block = loc.getBlock();
                        Material type = block.material;

                        if (type == Material.SOUL_SAND) {
                            player.getTrackers().add(Trackers.TrackerType.BubbleWater, (int) TPS.maximum);
                            player.getTrackers().add(Trackers.TrackerType.BubbleWater, "soul-sand", (int) TPS.maximum);
                            break;
                        } else if (type == MAGMA_BLOCK) {
                            player.getTrackers().add(Trackers.TrackerType.BubbleWater, (int) TPS.maximum);
                            player.getTrackers().add(Trackers.TrackerType.BubbleWater, "magma-block", (int) TPS.maximum);
                            break;
                        } else if (BlockUtils.isSolid(type) && !block.waterLogged) {
                            nonLiquid++;

                            if (nonLiquid == locations.length) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    // Separator

    public static boolean recentlyBounced(SpartanPlayer p) {
        return v1_8 && p.getTrackers().has(Trackers.TrackerType.BouncingBlocks);
    }

    public static boolean canCheck(SpartanPlayer player) {
        if (!Attributes.has(player, Attributes.GENERIC_MOVEMENT_SPEED)
                && player.canRunChecks(true)
                && player.movement.getLastFlight() > (TPS.maximum * 2)
                && !AbstractVelocity.hasCooldown(player)) {
            if (Compatibility.CompatibilityType.MythicMobs.isFunctional()
                    || Compatibility.CompatibilityType.ItemsAdder.isFunctional()) {
                List<Entity> entities = player.getNearbyEntities(
                        CombatUtils.maxHitDistance,
                        CombatUtils.maxHitDistance,
                        CombatUtils.maxHitDistance
                );

                if (!entities.isEmpty()) {
                    for (Entity entity : entities) {
                        if (MythicMobs.is(entity) || ItemsAdder.is(entity)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static int hashTotal(int player, int environment, int trackers) {
        return HashUtils.extendInt(HashUtils.extendInt(player, environment), trackers);
    }

    public static int artificialPlayerHash(
            boolean sneak,
            boolean ground,
            int ping
    ) {
        return HashUtils.hashPlayer(
                null,
                GameMode.SURVIVAL,
                null,
                HashUtils.emptyEffects,
                ping,
                Long.MAX_VALUE,
                false,
                false,
                false,
                false,
                false,
                false,
                sneak,
                ground
        );
    }
}
