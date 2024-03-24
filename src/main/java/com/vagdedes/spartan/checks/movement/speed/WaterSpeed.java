package com.vagdedes.spartan.checks.movement.speed;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.Liquid;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.functionality.identifiers.simple.DetectionLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;

public class WaterSpeed extends DetectionExecutor {

    WaterSpeed(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    private static final String horizontalBufferKey = "buffer=horizontal";

    private static final int initialWaterLimit = 10;
    private static final double minDs = 0.1,
            out_water = 0.19, in_water = 0.225,
            lava_fire_ticks = 0.3, out_lava = 0.15, in_lava = 0.13,
            neutral = 0.45,
            bouncing = 0.35,
            walls = 0.28,
            premature = 0.6,
            stable = 0.17, stable_extra = 0.22,
            instability = 0.3;

    private static final double[][] ice = new double[][]{
            {0, -1, 0},
            {0, 0, 0},
            {0, 1, 0}
    };

    private static final double[] genericHitbox = new double[]{
            BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max
    };

    void run() {
        Double horizontal = player.getNmsHorizontalDistance(),
                vertical = player.getNmsVerticalDistance(),
                oldVertical = player.getPreviousNmsVerticalDistance(),
                box = player.getNmsBox();

        if (horizontal == null
                || vertical == null
                || oldVertical == null
                || box == null
                || horizontal < minDs && Math.abs(vertical) < minDs) {
            return;
        }
        SpartanLocation to = player.getEventToLocation(),
                from = player.getEventFromLocation();
        int airTicks = player.getTicksOnAir();
        boolean selfHit = Damage.hasCooldown(player, Damage.selfHitKey);
        Buffer buffer = player.getBuffer(hackType);
        Cooldowns cooldowns = player.getCooldowns(hackType);
        int horizontalBuffer = buffer.get(horizontalBufferKey);
        SpartanLocation loc1 = to.clone().add(0, 1, 0);

        if (!canDo(to, loc1, cooldowns, selfHit)) {
            freezeBuffers(buffer, 1);
            return;
        }
        if (to.getBlock().waterLogged
                || from.getBlock().waterLogged
                || to.clone().add(0, -0.5, 0).getBlock().waterLogged) {
            freezeBuffers(buffer, 1);
            return;
        }

        runAsync(() -> {
            int horizontalBufferCopy = horizontalBuffer;
            runTrackers(to, cooldowns, buffer, airTicks, vertical, box);
            boolean boatStepping = player.stepsOnBoats();

            if (boatStepping) {
                horizontalBufferCopy = freezeBuffers(buffer, 2);
            }
            SpartanLocation locM1 = to.clone().add(0, -1, 0),
                    locM2 = to.clone().add(0, -2, 0);
            boolean suspicion,
                    clearMovement,
                    isSwimming = !player.getHandlers().has(Handlers.HandlerType.Vehicle, "non-boat") && player.isSwimming();

            if (player.isJumping(vertical)) {
                suspicion = false;
                clearMovement = true;
            } else {
                suspicion = vertical >= -0.07 && !PatternUtils.isLiquidPattern(new double[]{BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max}, to, true);
                clearMovement = suspicion || player.getProfile().isSuspectedOrHacker(hackType);
            }
            double limit = 0.0;
            int limitType = Integer.MIN_VALUE;
            boolean canCheckWater = false;

            if (canCheck(BlockUtils.water, box, to, loc1, locM1, locM2, buffer) ? (canCheckWater = true) :
                    canCheck(BlockUtils.lava, box, to, loc1, locM1, locM2, buffer)) {
                Material material = canCheckWater ? BlockUtils.water : BlockUtils.lava;
                boolean wallsBelow;
                limitType = canCheckWater ? initialWaterLimit : 0;

                if (areWalls(locM1, to, loc1)) {
                    wallsBelow = true;
                    cooldowns.add("walls-below", 10);
                } else {
                    wallsBelow = !cooldowns.canDo("walls-below");
                }

                if (PlayerUtils.getWalkSpeedDifference(player) != 0.0) {
                    limit = premature;
                } else if (BouncingBlocks.isBelow(player)) {
                    limit = bouncing;
                    limitType += 1;
                } else if (wallsBelow && Math.abs(vertical - oldVertical) >= 0.01) {
                    limit = walls;
                    limitType += 2;
                } else {
                    int bootsLevel = PlayerUtils.getDepthStriderLevel(player);

                    if (vertical == 0.0 && horizontalBufferCopy >= 10) {
                        limit = (player.isSprinting() ? stable_extra : stable);
                        limit *= bootsLevel;
                        limitType += 3;
                    } else if (to.getBlock().material == material) {
                        if (clearMovement) { // Clear Movement
                            if (canCheckWater) {
                                limit = in_water + (bootsLevel * 3.5);
                                limitType += 4;
                            } else {
                                if (!player.isOnFire()) {
                                    limit = in_lava;
                                    limitType += 4;
                                } else {
                                    limit = lava_fire_ticks;
                                    limitType += 5;
                                }
                            }
                        } else {
                            if (canCheckWater) {
                                limit = neutral + (bootsLevel * 1.75);
                                limitType += 5;
                            } else {
                                limit = neutral;
                                limitType += 6;
                            }
                        }

                    } else if (locM1.areBlocks(material, 1.0, true) && isSolidAndLiquidFree(to, 1, 0, 1)
                            || locM2.areBlocks(material, 1.0, true) && isSolidAndLiquidFree(to, 1, 0, 1) && isSolidAndLiquidFree(to, 1, -1, 1)) {
                        if (canCheckWater) {
                            limit = out_water;
                            limitType += 6;
                        } else {
                            if (!player.isOnFire()) {
                                limit = out_lava;
                                limitType += 7;
                            } else {
                                limit = lava_fire_ticks;
                                limitType += 8;
                            }
                        }
                    } else {
                        limit = neutral;

                        if (canCheckWater) {
                            limitType += 8;
                        } else {
                            limitType = initialWaterLimit - 1;
                        }
                    }
                }
            } else if (!suspicion && buffer.start("cannot-check-block", 20) >= 3) {
                horizontalBufferCopy = freezeBuffers(buffer, 1);
            }
            runBuffers(to, locM1, horizontal, vertical, buffer);

            if (limit > 0.0) {
                Entity vehicle = player.getVehicle();
                double modifiedLimit = adjustLimit(to, locM1, loc1,
                        vehicle,
                        cooldowns,
                        horizontalBufferCopy,
                        limit, vertical,
                        boatStepping, isSwimming, selfHit);

                if (modifiedLimit > 0.0 && horizontal >= modifiedLimit && cooldowns.canDo("allow")) {
                    boolean cancel;

                    if (limit == out_water) {
                        cancel = (horizontal - limit) >= 0.35 || buffer.start("water=safety=max", 20) >= 5;
                    } else {
                        cancel = limit != modifiedLimit
                                || player.getViolations(hackType).hasLevel()
                                || buffer.start("water=safety=min", 12) >= 4;
                    }

                    if (cancel) {
                        cancel(
                                "type: water, vertical: " + vertical
                                        + ", horizontal: " + horizontal
                                        + ", limit: " + limit + "(" + limitType + ")"
                                        + ", dynamic-limit: " + modifiedLimit
                                        + (vehicle != null ? ", entity: " + vehicle.getType().toString().toLowerCase().replace("_", "-") : ""),
                                DetectionLocation.get(player, true).clone().add(0, -box, 0)
                        );
                    }
                }
            }
        });
    }

    // Cache

    private int freezeBuffers(Buffer buffer, int divisor) {
        int finalBuffer = -(PlayerUtils.getWalkSpeedDifference(player) > 0.0f ? 40 : (20 / divisor));
        buffer.set(horizontalBufferKey, finalBuffer);
        return finalBuffer;
    }

    private void runBuffers(SpartanLocation loc, SpartanLocation locM1,
                            double horizontal, double vertical,
                            Buffer buffer) {
        int increase;

        if (vertical >= -0.07 && !player.isJumping(vertical)

                && (AlgebraUtils.cut(vertical, 1) != 0.0

                || isSolidAndLiquidFree(loc, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                && isSolidAndLiquidFree(locM1, BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max))) {
            increase = 3;
        } else {
            increase = 1;
        }

        if (horizontal > 0.0) {
            buffer.increase(horizontalBufferKey, increase);
        }
    }

    private void runTrackers(SpartanLocation to,
                             Cooldowns cooldowns, Buffer buffer,
                             int airTicks,
                             double dy, double remaining) {
        if (airTicks >= 100) {
            cooldowns.add("allow", 40);
        } else if (dy <= -2.0) {
            cooldowns.add("allow", 20);
        } else if (PatternUtils.isBlockPattern(ice, to, false, BlockUtils.ice)) {
            if (PatternUtils.isBlockPattern(new double[][]{
                    {1, 2, 1},
                    {1, 2 - remaining, 1}
            }, to, true, BlockUtils.solid, BlockUtils.semi_solid)) { // Blocks above
                cooldowns.add("ice", 20);
            } else if (player.wasInLiquids() || !Liquid.isLocation(player, to)) {
                freezeBuffers(buffer, 1);
            }
        }
    }

    // Base

    private boolean canCheck(Material m, double remaining,
                             SpartanLocation to, SpartanLocation loc1, SpartanLocation locM1, SpartanLocation locM2,
                             Buffer buffer) {
        if ((!isFullSolidFree(locM1) && !PatternUtils.isLiquidPattern(genericHitbox, to, true)
                || !isFullSolidFree(to))

                && !PatternUtils.isLiquidPattern(genericHitbox, loc1, true)) {
            freezeBuffers(buffer, 1);
            return false;
        }
        return loc1.getBlock().material == m
                || to.areBlocks(m, BlockUtils.hitbox_max, true)
                || locM1.areBlocks(m, BlockUtils.hitbox_max, true)
                || to.clone().add(0, -remaining, 0).areBlocks(m, BlockUtils.hitbox_max, true)
                || !PatternUtils.isBlockPattern(genericHitbox, locM1, true, BlockUtils.solid, BlockUtils.semi_solid) && locM2.areBlocks(m, BlockUtils.hitbox_max, true);
    }

    private double adjustLimit(SpartanLocation loc, SpartanLocation locM1, SpartanLocation loc1,
                               Entity vehicle,
                               Cooldowns cooldowns,
                               int horizontalBuffer,
                               double limit, double vertical,
                               boolean boatStepping, boolean isSwimming, boolean selfHit) {
        double modifiedLimit = limit;

        // Separator
        if (player.getHandlers().has(Handlers.HandlerType.Trident)) {
            modifiedLimit = Math.max(modifiedLimit, Math.max(PlayerUtils.getWalkSpeedDifference(player), 4.5));
        } else if (player.getHandlers().has(Handlers.HandlerType.Piston)) {
            modifiedLimit = Math.max(modifiedLimit, Math.max(PlayerUtils.getWalkSpeedDifference(player), Speed.piston));
        } else if (boatStepping || selfHit || horizontalBuffer < 0 || vehicle instanceof Horse
                || !cooldowns.canDo("ice") || player.getHandlers().has(Handlers.HandlerType.Explosion)) {
            modifiedLimit = Math.max(modifiedLimit, Math.max(PlayerUtils.getWalkSpeedDifference(player), premature));
        } else if (player.bedrockPlayer
                || Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()
                || player.getHandlers().has(Handlers.HandlerType.TowerBuilding)) {
            modifiedLimit = Math.max(modifiedLimit, instability);
        }

        // Separator
        if (limit != out_lava && limit != in_lava && limit != lava_fire_ticks) {
            if (PlayerUtils.hasDolphinsGraceEffect(player)) {
                double depthStrider = PlayerUtils.getDepthStriderLevel(player);

                if (PlayerUtils.hasSoulSpeedEnchantment(player, loc)) {
                    modifiedLimit += (depthStrider + 1) * 2.0;
                } else {
                    modifiedLimit += Math.max(depthStrider, 0.5);
                }
            } else if (isSwimming) {
                if (limit != stable && limit != stable_extra) {
                    modifiedLimit += 0.25;
                } else if (BlockUtils.isSolid(locM1)) {
                    modifiedLimit += (BlockUtils.isSolid(loc1) ? 0.03 : 0.07);
                } else {
                    modifiedLimit += 0.05;
                }
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    && limit != in_water && limit != bouncing && limit != walls
                    && (horizontalBuffer <= 5 || vertical != 0.0)
                    && !player.isJumping(vertical) && !player.isFalling(vertical)) {
                modifiedLimit = Math.max(modifiedLimit, instability);
            }
        }

        // Separator
        modifiedLimit = PlayerUtils.calculateLimit(player, loc, modifiedLimit, 8.0, PotionEffectType.SPEED);
        double minimumLimit = hackType.getCheck().getDecimalOption("minimum_limit_in_blocks", 0.0);

        if (minimumLimit < 0.5 && vehicle != null) {
            minimumLimit = 0.5;
        }
        return Math.max(minimumLimit, modifiedLimit);
    }

    private boolean canDo(SpartanLocation to, SpartanLocation loc1, Cooldowns cooldowns, boolean selfHit) {
        if (player.canRunCheck(hackType, true) && hackType.getCheck().getBooleanOption("check_water_speed", true)
                && (selfHit || !Damage.hasCooldown(player)) && !PlayerUtils.hasNearbyCollisions(player) && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                && !PlayerUtils.hasLevitationEffect(player) && !player.getHandlers().has(Handlers.HandlerType.SensitiveBlockBreak)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                Entity vehicle = player.getVehicle();

                if (vehicle != null
                        && !(vehicle instanceof Boat)
                        && !(vehicle instanceof Pig)
                        && !(vehicle instanceof Horse)
                        && !(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11) && vehicle instanceof Llama)
                        && !(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20) && vehicle instanceof Camel)) {
                    return false;
                }
            }
            if (!BlockUtils.isClearLiquid(player, to) || !BlockUtils.isClearLiquid(player, loc1)) {
                cooldowns.add("invalid=data", 10);
                return false;
            }
            return cooldowns.canDo("invalid=data");
        }
        return false;
    }

    // Utils

    private boolean isSolidAndLiquidFree(SpartanLocation loc, double x, double y, double z) {
        return !PatternUtils.isBlockPattern(new double[]{x, y, z}, loc, true, BlockUtils.solid, BlockUtils.semi_solid)
                && !PatternUtils.isLiquidPattern(new double[]{x, y, z}, loc, true);
    }

    private boolean isFullSolidFree(SpartanLocation loc) {
        return !PatternUtils.isBlockPattern(genericHitbox, loc, true, BlockUtils.solid, BlockUtils.semi_solid);
    }

    private static boolean areWalls(SpartanLocation locM1, SpartanLocation to, SpartanLocation loc1) {
        return !BlockUtils.isSolid(loc1) && !BlockUtils.isSolid(to.clone().add(0, 2, 0))
                && PatternUtils.isLiquidPattern(genericHitbox, to, true)
                && PatternUtils.isBlockPattern(genericHitbox, locM1, true, BlockUtils.walls);
    }
}
