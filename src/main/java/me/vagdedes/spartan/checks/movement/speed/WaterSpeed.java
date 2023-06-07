package me.vagdedes.spartan.checks.movement.speed;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.features.protections.Building;
import me.vagdedes.spartan.handlers.identifiers.*;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPreventionUtils;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Pig;
import org.bukkit.potion.PotionEffectType;

public class WaterSpeed {

    private static final Material water = MaterialUtils.get("water");
    private static final Material lava = MaterialUtils.get("lava");
    private static final String horizontalBufferKey = "buffer=horizontal";

    private static final int initialWaterLimit = 10;
    private static final double minDs = 0.1;
    private static final double out_water = 0.19, in_water = 0.225;
    private static final double lava_fire_ticks = 0.3, out_lava = 0.15, in_lava = 0.13;
    private static final double neutral = 0.45,
            bouncing = 0.35,
            walls = 0.28,
            premature = 0.6,
            stable = 0.17, stable_extra = 0.22,
            instability = 0.3;

    public static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                           double vertical, double oldVertical, double box, double horizontal,
                           int airTicks) {
        if (horizontal < minDs && Math.abs(vertical) < minDs) {
            return;
        }
        boolean selfHit = Damage.hasCooldown(p, Damage.selfHitKey);
        Buffer buffer = p.getBuffer(Speed.check);
        Cooldowns cooldowns = p.getCooldowns(Speed.check);
        int horizontalBuffer = buffer.get(horizontalBufferKey);
        SpartanLocation loc1 = to.clone().add(0, 1, 0);

        if (!canDo(p, to, loc1, cooldowns, selfHit)) {
            freezeBuffers(p, buffer, 1);
            return;
        }
        if (to.getBlock().isWaterLogged()
                || from.getBlock().isWaterLogged()
                || to.clone().add(0, -0.5, 0).getBlock().isWaterLogged()) {
            freezeBuffers(p, buffer, 1);
            return;
        }

        AsyncDetection.run(p, Speed.check, () -> {
            int horizontalBufferCopy = horizontalBuffer;
            runTrackers(p, to, cooldowns, buffer, airTicks, vertical, box);
            boolean boatStepping = p.stepsOnBoats();

            if (boatStepping) {
                horizontalBufferCopy = freezeBuffers(p, buffer, 2);
            }
            SpartanLocation locM1 = to.clone().add(0, -1, 0),
                    locM2 = to.clone().add(0, -2, 0);
            boolean suspicion,
                    clearMovement,
                    isSwimming = !p.getHandlers().has(Handlers.HandlerType.Vehicle, "non-boat") && p.isSwimming();

            if (p.isJumping(vertical)) {
                suspicion = false;
                clearMovement = true;
            } else {
                suspicion = vertical >= -0.07 && BlockUtils.isLiquidFree(to, BlockUtils.hitbox_max, 1, BlockUtils.hitbox_max);
                clearMovement = suspicion || p.getProfile().isSuspectedOrHacker(Speed.check);
            }
            double limit = 0.0;
            int limitType = Integer.MIN_VALUE;
            boolean canCheckWater = false;

            if (canCheck(p, water, box, to, loc1, locM1, locM2, buffer) ? (canCheckWater = true) :
                    canCheck(p, lava, box, to, loc1, locM1, locM2, buffer)) {
                Material material = canCheckWater ? water : lava;
                boolean wallsBelow;
                limitType = canCheckWater ? initialWaterLimit : 0;

                if (areWalls(locM1, to, loc1)) {
                    wallsBelow = true;
                    cooldowns.add("walls-below", 10);
                } else {
                    wallsBelow = !cooldowns.canDo("walls-below");
                }

                if (PlayerData.getWalkSpeedDifference(p) != 0.0) {
                    limit = premature;
                } else if (BouncingBlocks.isBelow(p)) {
                    limit = bouncing;
                    limitType += 1;
                } else if (wallsBelow && Math.abs(vertical - oldVertical) >= 0.01) {
                    limit = walls;
                    limitType += 2;
                } else {
                    int bootsLevel = PlayerData.getDepthStriderLevel(p);

                    if (vertical == 0.0 && horizontalBufferCopy >= 10) {
                        limit = (p.isSprinting() ? stable_extra : stable);
                        limit *= bootsLevel;
                        limitType += 3;
                    } else if (to.getBlock().getType() == material) {
                        if (clearMovement) { // Clear Movement
                            if (canCheckWater) {
                                limit = in_water + (bootsLevel * 3.5);
                                limitType += 4;
                            } else {
                                if (!p.isOnFire()) {
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

                    } else if (locM1.areBlocks(material, 1.0) && isSolidFree(to, 1, 0, 1)
                            || locM2.areBlocks(material, 1.0) && isSolidFree(to, 1, 0, 1) && isSolidFree(to, 1, -1, 1)) {
                        if (canCheckWater) {
                            limit = out_water;
                            limitType += 6;
                        } else {
                            if (!p.isOnFire()) {
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
                horizontalBufferCopy = freezeBuffers(p, buffer, 1);
            }
            runBuffers(p, to, locM1, horizontal, vertical, buffer);

            if (limit > 0.0) {
                Entity vehicle = p.getVehicle();
                double modifiedLimit = adjustLimit(p, to, locM1, loc1,
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
                                || Speed.check.getCheck().getViolations(p).hasLevel()
                                || buffer.start("water=safety=min", 12) >= 4;
                    }

                    if (cancel) {
                        HackPreventionUtils.preventVelocity(p);
                        new HackPrevention(p, Speed.check,
                                "type: water, vertical: " + vertical
                                        + ", horizontal: " + horizontal
                                        + ", limit: " + limit + "(" + limitType + ")"
                                        + ", dynamic-limit: " + modifiedLimit
                                        + (vehicle != null ? ", entity: " + vehicle.getType().toString().toLowerCase().replace("_", "-") : ""),
                                from.clone().add(0, -box, 0), 0, true, 0.0, true);
                    }
                }
            }
        });
    }

    // Cache

    private static int freezeBuffers(SpartanPlayer p, Buffer buffer, int divisor) {
        int finalBuffer = -(PlayerData.getWalkSpeedDifference(p) > 0.0f ? 40 : (20 / divisor));
        buffer.set(horizontalBufferKey, finalBuffer);
        return finalBuffer;
    }

    private static void runBuffers(SpartanPlayer p, SpartanLocation loc, SpartanLocation locM1,
                                   double horizontal, double vertical,
                                   Buffer buffer) {
        int increase;

        if (vertical >= -0.07 && !p.isJumping(vertical)

                && (AlgebraUtils.cut(vertical, 1) != 0.0

                || isSolidFree(loc, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                && isSolidFree(locM1, BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max))) {
            increase = 3;
        } else {
            increase = 1;
        }

        if (horizontal > 0.0) {
            buffer.increase(horizontalBufferKey, increase);
        }
    }

    private static void runTrackers(SpartanPlayer p, SpartanLocation to,
                                    Cooldowns cooldowns, Buffer buffer,
                                    int airTicks,
                                    double dy, double remaining) {
        if (airTicks >= 100) {
            cooldowns.add("allow", 40);
        } else if (dy <= -2.0) {
            cooldowns.add("allow", 20);
        } else {
            boolean blocksAbove = !BlockUtils.isSolidFree(to, 1, 2, 1) || !BlockUtils.isSolidFree(to, 1, 2 - remaining, 1);

            for (int i = -1; i <= 0; i++) {
                if (BlockUtils.areIceBlocks(to.clone().add(0, i, 0))) {
                    if (blocksAbove) {
                        cooldowns.add("ice", 20);
                    } else if (p.wasInLiquids() || !Liquid.isLocation(p, to)) { // Attention, added recently
                        freezeBuffers(p, buffer, 1);
                    }
                    return;
                }
            }
        }
    }

    // Base

    private static boolean canCheck(SpartanPlayer p, Material m, double remaining,
                                    SpartanLocation to, SpartanLocation loc1, SpartanLocation locM1, SpartanLocation locM2,
                                    Buffer buffer) {
        if ((!isNonSolidFree(locM1) && BlockUtils.isLiquidFree(to, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                || !isNonSolidFree(to))

                && BlockUtils.isLiquidFree(loc1, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)) {
            freezeBuffers(p, buffer, 1);
            return false;
        }
        return loc1.getBlock().getType() == m
                || to.areBlocks(m, BlockUtils.hitbox_max)
                || locM1.areBlocks(m, BlockUtils.hitbox_max)
                || to.clone().add(0, -remaining, 0).areBlocks(m, BlockUtils.hitbox_max)
                || BlockUtils.isSolidFree(locM1, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max) && locM2.areBlocks(m, BlockUtils.hitbox_max);
    }

    private static double adjustLimit(SpartanPlayer p, SpartanLocation loc, SpartanLocation locM1, SpartanLocation loc1,
                                      Entity vehicle,
                                      Cooldowns cooldowns,
                                      int horizontalBuffer,
                                      double limit, double vertical,
                                      boolean boatStepping, boolean isSwimming, boolean selfHit) {
        double modifiedLimit = limit;

        // Separator
        if (p.getHandlers().has(Handlers.HandlerType.Trident)) {
            modifiedLimit = Math.max(modifiedLimit, Math.max(PlayerData.getWalkSpeedDifference(p), 4.5));
        } else if (p.getHandlers().has(Handlers.HandlerType.Piston)) {
            modifiedLimit = Math.max(modifiedLimit, Math.max(PlayerData.getWalkSpeedDifference(p), Speed.piston));
        } else if (boatStepping || selfHit || horizontalBuffer < 0 || vehicle instanceof Horse
                || !cooldowns.canDo("ice") || p.getHandlers().has(Handlers.HandlerType.Explosion)) {
            modifiedLimit = Math.max(modifiedLimit, Math.max(PlayerData.getWalkSpeedDifference(p), premature));
        } else if (Compatibility.CompatibilityType.MajorIncompatibility.isFunctional() || p.isBedrockPlayer() || Building.hasCooldown(p)) {
            modifiedLimit = Math.max(modifiedLimit, instability);
        }

        // Separator
        if (limit != out_lava && limit != in_lava && limit != lava_fire_ticks) {
            if (PlayerData.hasDolphinsGraceEffect(p)) {
                double depthStrider = PlayerData.getDepthStriderLevel(p);

                if (PlayerData.hasSoulSpeedEnchantment(p, loc)) {
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
                    && !p.isJumping(vertical) && !p.isFalling(vertical)) {
                modifiedLimit = Math.max(modifiedLimit, instability);
            }
        }

        // Separator
        modifiedLimit = MoveUtils.calculateLimit(p, loc, modifiedLimit, 8.0, PotionEffectType.SPEED);
        return Math.max(Speed.check.getCheck().getDecimalOption("minimum_limit_in_blocks", 0.0), modifiedLimit);
    }

    private static boolean canDo(SpartanPlayer p, SpartanLocation to, SpartanLocation loc1, Cooldowns cooldowns, boolean selfHit) {
        if (p.canRunCheck(Speed.check, true) && Speed.check.getCheck().getBooleanOption("check_water_speed", true)
                && (selfHit || !Damage.hasCooldown(p)) && !PlayerData.hasNearbyCollisions(p) && !p.getHandlers().has(Handlers.HandlerType.ElytraUse)
                && !PlayerData.hasLevitationEffect(p) && !FishingHook.hasCooldown(p) && !p.getHandlers().has(Handlers.HandlerType.BlockBreak)
                && !VehicleAccess.hasEnterCooldown(p, Speed.check)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                Entity vehicle = p.getVehicle();

                // Do not check boats, they behave much differently on water and require their own detection via the EntityMove check
                if (vehicle != null
                        && !(vehicle instanceof Pig)
                        && !(vehicle instanceof Horse)
                        && !(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11) && vehicle instanceof Llama)) {
                    return false;
                }
            }
            if (!BlockUtils.isClearLiquid(p, to) || !BlockUtils.isClearLiquid(p, loc1)) {
                cooldowns.add("invalid=data", 10);
                return false;
            }
            return cooldowns.canDo("invalid=data");
        }
        return false;
    }

    // Utils

    private static boolean isSolidFree(SpartanLocation loc, double x, double y, double z) {
        return BlockUtils.isSolidFree(loc, x, y, z) && BlockUtils.isLiquidFree(loc, x, y, z);
    }

    private static boolean isNonSolidFree(SpartanLocation loc) {
        for (SpartanLocation locs : loc.getSurroundingLocations(BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)) {
            if (BlockUtils.isSolid(locs) || BlockUtils.isSemiSolid(locs)) {
                return false;
            }
        }
        return true;
    }

    private static boolean areWalls(SpartanLocation locM1, SpartanLocation to, SpartanLocation loc1) {
        return !BlockUtils.isSolid(loc1) && !BlockUtils.isSolid(to.clone().add(0, 2, 0))
                && !BlockUtils.isLiquidFree(to, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max)
                && BlockUtils.areWallsAround(locM1, BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max);
    }
}
