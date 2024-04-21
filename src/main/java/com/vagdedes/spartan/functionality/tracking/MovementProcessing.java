package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MovementProcessing {

    private static final boolean
            v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8),
            v1_9 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9);
    private static final int roundedFall = AlgebraUtils.integerCeil(PlayerUtils.terminalVelocity);
    private static final Material MAGMA_BLOCK = MaterialUtils.get("magma");

    public static void run(Player n, SpartanPlayer player,
                           SpartanLocation location,
                           double distance, double horizontal,
                           double vertical, double box,
                           float fall) {
        GameMode current = player.getGameMode();

        if (current == GameMode.CREATIVE
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && current == GameMode.SPECTATOR) {
            player.getHandlers().add(Handlers.HandlerType.GameMode, 60);
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

        if (!calculateLiquid(player, location)) {
            if (Math.abs(PlayerUtils.jumping[1] - vertical) < PlayerUtils.getJumpingPrecision(player)) { // Last Jump
                player.movement.setLastJump();
            } else if (player.movement.isFalling(vertical)) { // Last Fall
                player.movement.setLastFall();
            }
            if (!calculateGroundCollision(player, vertical, box)) {
                calculateBouncing(player, location, vertical);
            }
        } else {
            calculateBouncing(player, location, vertical);
        }
        calculateExtremeCollision(player, location);

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
    }

    private static boolean calculateGroundCollision(SpartanPlayer player,
                                                    double vertical, double box) {
        if (player.isOnGroundCustom()
                && player.isOnGround()
                && player.movement.getTicksOnAir() == 0
                && player.getVehicle() == null
                && vertical == 0.0
                && GroundUtils.collisionHeightExists(box)) {
            Handlers handlers = player.getHandlers();
            handlers.removeMany(Handlers.HandlerFamily.Motion);
            handlers.removeMany(Handlers.HandlerFamily.Velocity);
            player.movement.removeLastLiquidTime();
            return true;
        } else {
            return false;
        }
    }

    private static void calculateExtremeCollision(SpartanPlayer player, SpartanLocation location) {
        if (v1_9) {
            Handlers handlers = player.getHandlers();

            if (!handlers.isDisabled(Handlers.HandlerType.ExtremeCollision)) {
                handlers.disable(Handlers.HandlerType.ExtremeCollision, 2); // Disable for the next tick
                int collisions = PlayerUtils.getNearbyCollisions(player, location);

                if (collisions > 10) {
                    handlers.add(Handlers.HandlerType.ExtremeCollision, Math.min((int) (TPS.maximum * 2), collisions));
                }
            }
        }
    }

    private static void calculateBouncing(SpartanPlayer player, SpartanLocation location,
                                          double vertical) {
        if (v1_8 && vertical != 0.0) {
            if (BlockUtils.isSlime(player, location, roundedFall)) {
                int time = (int) (TPS.maximum * 2);
                player.getHandlers().add(Handlers.HandlerType.BouncingBlocks, time);
                player.getHandlers().add(Handlers.HandlerType.BouncingBlocks, "slime", time);
            } else if (BlockUtils.isBed(player, location, roundedFall)) {
                int time = (int) (TPS.maximum * 2);
                player.getHandlers().add(Handlers.HandlerType.BouncingBlocks, time);
                player.getHandlers().add(Handlers.HandlerType.BouncingBlocks, "bed", time);
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
                            player.getHandlers().add(Handlers.HandlerType.BubbleWater, (int) TPS.maximum);
                            player.getHandlers().add(Handlers.HandlerType.BubbleWater, "soul-sand", (int) TPS.maximum);
                            break;
                        } else if (type == MAGMA_BLOCK) {
                            player.getHandlers().add(Handlers.HandlerType.BubbleWater, (int) TPS.maximum);
                            player.getHandlers().add(Handlers.HandlerType.BubbleWater, "magma-block", (int) TPS.maximum);
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
        return v1_8 && p.getHandlers().has(Handlers.HandlerType.BouncingBlocks);
    }

}
