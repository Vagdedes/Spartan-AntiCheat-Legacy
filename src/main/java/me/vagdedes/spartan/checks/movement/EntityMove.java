package me.vagdedes.spartan.checks.movement;

import me.vagdedes.spartan.compatibility.manual.entity.CraftBook;
import me.vagdedes.spartan.compatibility.manual.entity.MyPet;
import me.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Cache;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.*;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class EntityMove {

    public static final Enums.HackType check = Enums.HackType.EntityMove;

    private static final int liquidCacheTicks = 30, iceCacheTicks = 30,
            horseID = 1, boatID = 2, pigID = 3, llamaID = 4, camelID = 5,
            horseJumpPositiveMotions = 5,
            livingEntityBufferTicks = 20, livingEntityBufferFails = 3,
            boatBufferTicks = 20, boatBufferFails = 3;

    private static final double horseMinimumHorizontal = 0.5,
            pigMinimumHorizontal = 0.15, pigCarrotMinimumHorizontal = 0.3,
            boatMotionPrecision = 0.001;

    private static final Map<UUID, Long[]> validity = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
    private static final int[] entityIDs = new int[]{
            horseID, boatID, pigID, llamaID, camelID
    };

    private static final double[][]
            ice = new double[][]{
            {BlockUtils.hitbox_max, -1, BlockUtils.hitbox_max},
            {BlockUtils.hitbox_max, 0, BlockUtils.hitbox_max},
    }, web = new double[][]{
            {1, -1, 0},
            {1, 0, 1},
            {1, 1, 1}
    }, pig = new double[][]{
            {1, -1, 0},
            {1, 0, 1},
    };

    private static final Material
            piston = MaterialUtils.get("piston"),
            sticky_piston = MaterialUtils.get("sticky_piston"),
            piston_extension = MaterialUtils.get("piston_extension"),
            piston_moving = MaterialUtils.get("piston_moving"),
            carrotOnAStick = MaterialUtils.get("carrot_on_a_stick");

    public static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                           double horizontal, double vertical, double oldVertical, double box, double oldBox,
                           int airTicks) {
        UUID uuid = p.getUniqueId();
        Entity vehicle = p.getVehicle();

        if (vehicle != null) {
            long entityLongID = canDo(p, to, vehicle);

            if (entityLongID != 0) {
                Long[] cache = validity.get(uuid);

                if (cache == null) {
                    validity.put(uuid, new Long[]{entityLongID, System.currentTimeMillis()});
                    return;
                } else if (cache[0] != entityLongID) {
                    validity.put(uuid, new Long[]{entityLongID, System.currentTimeMillis()});
                    check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), 4);
                    return;
                }
                int entityID = (int) entityLongID;
                long time = System.currentTimeMillis() - cache[1];
                List<SpartanLocation> liquidBlocks = getLiquidBlocks(
                        to,
                        Math.max(CombatUtils.getWidthAndHeight(vehicle)[0], BlockUtils.hitbox)
                );
                boolean unstableLiquids = cacheBoolean(p, false, "unstable-liquids", liquidCacheTicks),
                        liquidsLocal = !liquidBlocks.isEmpty(),
                        liquids = cacheBoolean(p, liquidsLocal, "liquids", liquidCacheTicks) || p.wasInLiquids(),
                        iceBlocks = cacheBoolean(p,
                                PatternUtils.isBlockPattern(ice, to, true, BlockUtils.ice),
                                "ice-blocks", iceCacheTicks),
                        damage = Damage.hasCooldown(p) || p.getHandlers().has(Handlers.HandlerType.Explosion),
                        knownBoxes = GroundUtils.heightExists(box),
                        oldKnownBoxes = GroundUtils.heightExists(oldBox),
                        checkVertical = check.getCheck().getBooleanOption("check_vertical", true),
                        checkHorizontal = check.getCheck().getBooleanOption("check_horizontal", true);
                double verticalDifference = Math.abs(oldVertical - vertical);
                String message = "vertical: " + vertical
                        + ", old-vertical: " + oldVertical
                        + ", vertical-difference: " + verticalDifference
                        + ", horizontal: " + horizontal
                        + ", box: " + box
                        + ", old-box: " + oldBox
                        + ", air: " + airTicks
                        + ", time: " + time,
                        key = entityID + "=";
                Decimals decimals = p.getDecimals(check);

                if (liquidsLocal) {
                    for (SpartanLocation liquid : liquidBlocks) {
                        if (!BlockUtils.isClearLiquid(null, liquid)) {
                            unstableLiquids = cacheBoolean(p, true, "unstable-liquids", liquidCacheTicks);
                            break;
                        }
                    }
                }

                switch (entityID) {
                    case horseID:
                    case llamaID:
                        AsyncDetection.run(p, check, () -> {
                            clearLimitCache(p.getDecimals(check)); // Attention

                            if (!Damage.hasCooldown(p)) {
                                String messageCopy = message;
                                AbstractHorse abstractHorse = (AbstractHorse) vehicle;

                                if (checkVertical) {
                                    double verticalLimit = abstractHorse.getJumpStrength() * 1.1;

                                    if (knownBoxes) {
                                        verticalLimit = Math.max(verticalLimit, 1.0);

                                        if (vertical > verticalLimit) {
                                            int cases;

                                            if (horizontal < 1.0) { // when horizontal movements are low, high vertical movements are very unlikely
                                                cases = 1;
                                            } else if (vertical >= 3.0) { // too blatant motion to be considered legitimate
                                                cases = 2;
                                            } else if (vertical % box != 0.0 && (!oldKnownBoxes || vertical % oldBox != 0.0)) { // vertical movements will often follow bounding boxes
                                                cases = 3;
                                            } else if (airTicks > (horseJumpPositiveMotions + 1)) { // "5" was the last positive motion when jumping in debug, but we add 1 for stability
                                                cases = 4;
                                            } else if (oldVertical < 0.0) { // High vertical movements with previous negative movements are very unlikely
                                                cases = 5;
                                            } else {
                                                cases = 0;
                                            }

                                            if (cases != 0) {
                                                messageCopy += ", extra: " + cases + ", extra-1: " + verticalLimit;
                                                cancel(p, vehicle, from, "rapid-upwards", messageCopy);
                                            }
                                        }
                                    } else if (!liquids) {
                                        if (vertical > verticalLimit) {
                                            messageCopy += ", extra: " + verticalLimit;
                                            cancel(p, vehicle, from, "abstract-upwards", messageCopy);
                                        } else if (vertical > oldVertical) {
                                            if (oldVertical < 0.0) {
                                                if (!oldKnownBoxes
                                                        && !PatternUtils.isBlockPattern(web, to, true, BlockUtils.web)) {
                                                    cancel(p, vehicle, from, "illegal-upwards", messageCopy);
                                                }
                                            } else {
                                                cancelWithBuffer(p, vehicle, from, "irregular-upwards", messageCopy, key, livingEntityBufferTicks, livingEntityBufferFails);
                                            }
                                        }
                                    }
                                }
                                if (checkHorizontal) {
                                    AttributeInstance attribute = abstractHorse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);

                                    if (attribute != null) {
                                        double speedLimit = Math.max((attribute.getValue() * 43.1) / 20.0, horseMinimumHorizontal); // https://www.spigotmc.org/threads/361067/
                                        double multiplier = !liquids && airTicks <= horseJumpPositiveMotions && (knownBoxes && box >= 0.5 || oldKnownBoxes && oldBox >= 0.5) ? 3.0 : 2.0;
                                        speedLimit *= multiplier;

                                        if (horizontal >= speedLimit) {
                                            messageCopy += ", extra: " + speedLimit + ", extra-1: " + multiplier;
                                            cancel(p, vehicle, from, "horizontal", messageCopy);
                                        }
                                    }
                                }
                            }
                        });
                        break;
                    case pigID:
                        AsyncDetection.run(p, check, () -> {
                            clearLimitCache(p.getDecimals(check)); // Attention

                            if (!Damage.hasCooldown(p)) {
                                String messageCopy = message;
                                boolean notStepping = (!knownBoxes
                                        || !oldKnownBoxes
                                        || !PatternUtils.isBlockPattern(pig, to, true, BlockUtils.solid, BlockUtils.semi_solid));

                                if (checkVertical) {
                                    if (notStepping && vertical >= (damage ? 0.5 : 0.1)) {
                                        cancel(p, vehicle, from, "rapid-upwards", messageCopy);
                                    } else if (vertical > 0.0 && airTicks >= (damage ? 20 : 12)) {
                                        cancel(p, vehicle, from, "abstract-upwards", messageCopy);
                                    } else if (notStepping && verticalDifference >= (damage ? 0.5 : 0.2)) {
                                        cancel(p, vehicle, from, "irregular-upwards", messageCopy);
                                    } else if (vertical >= oldVertical && vertical != 0.0 && oldVertical != 0.0
                                            && !PatternUtils.isBlockPattern(web, to, true, BlockUtils.web)) {
                                        cancel(p, vehicle, from, "illegal-upwards", messageCopy);
                                    }
                                }
                                if (checkHorizontal) {
                                    double speedLimit;

                                    if (p.getItemInHand().getType() == carrotOnAStick
                                            || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && p.getInventory().getItemInOffHand().getType() == carrotOnAStick) {
                                        speedLimit = pigCarrotMinimumHorizontal;
                                    } else {
                                        speedLimit = pigMinimumHorizontal;
                                    }

                                    if (horizontal >= speedLimit) {
                                        messageCopy += ", extra: " + speedLimit;
                                        cancel(p, vehicle, from, "horizontal", messageCopy);
                                    }
                                }
                            }
                        });
                        break;
                    case boatID:
                        boolean unstableLiquidsCopy = unstableLiquids,
                                iceBlocksCopy = iceBlocks;

                        AsyncDetection.run(p, check, () -> {
                            String messageCopy = message;
                            boolean offGround = airTicks > 0 && !p.isOnGroundCustom()
                                    && (!knownBoxes && !oldKnownBoxes || !PlayerData.isOnGround(p, to, 0, -1, 0));

                            if (checkVertical && !BouncingBlocks.isBelow(p)) {
                                if (vertical > boatMotionPrecision) { // Upwards
                                    if (offGround && !p.getHandlers().has(Handlers.HandlerType.WaterElevator)) {
                                        if (vertical > oldVertical) { // Elevation
                                            if (!unstableLiquidsCopy) {
                                                cancelWithBuffer(p, vehicle, from, "elevation", messageCopy, key, boatBufferTicks, boatBufferFails);
                                            }
                                        } else if (vertical < oldVertical) { // Slower Elevation
                                            cancelWithBuffer(p, vehicle, from, "slower-elevation", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        } else { // Steady Elevation
                                            cancelWithBuffer(p, vehicle, from, "steady-elevation", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        }
                                    }
                                } else if (vertical < -boatMotionPrecision) { // Downwards
                                    if (vertical < oldVertical) { // Fall
                                        double divisor = 0.04;

                                        if (!liquids && Math.abs(Math.abs(vertical % divisor) - divisor) >= boatMotionPrecision) {
                                            cancelWithBuffer(p, vehicle, from, "modulo", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        } else if (offGround) {
                                            double difference = oldVertical - vertical;

                                            if (difference > (divisor + boatMotionPrecision)) {
                                                messageCopy += ", extra: " + difference;
                                                cancelWithBuffer(p, vehicle, from, "difference", messageCopy, key, boatBufferTicks, boatBufferFails);
                                            }
                                        }
                                    } else if ((vertical - oldVertical) > boatMotionPrecision) { // Slower Fall
                                        if (!liquids && offGround && !p.getHandlers().has(Handlers.HandlerType.WaterElevator)) {
                                            cancelWithBuffer(p, vehicle, from, "slow-fall", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        }
                                    } else { // Steady Fall
                                        if (!liquids && !p.getHandlers().has(Handlers.HandlerType.WaterElevator)) {
                                            cancelWithBuffer(p, vehicle, from, "steady-fall", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        }
                                    }
                                } else { // Neutral
                                    if (!liquids && offGround) {
                                        cancelWithBuffer(p, vehicle, from, "neutral", messageCopy, key, boatBufferTicks, boatBufferFails * 2);
                                    }
                                }
                            }
                            if (checkHorizontal) {
                                double limit = getGreaterRepetitiveLimit(decimals, entityID) + CraftBook.getBoatLimit();

                                if (iceBlocksCopy) {
                                    limit = cacheLimit(decimals, entityID, limit, 4.0, 20); // Actual speed is far too abstract to constantly predict
                                } else if (offGround || liquids || BlockUtils.hasNonSolidSide(to, 1.0, -1.0, 1.0, true)) {
                                    limit = cacheLimit(decimals, entityID, limit, 0.45, 10); // 0.4 max actual speed
                                } else {
                                    limit = cacheLimit(decimals, entityID, limit, 0.15, 0); // 0.1 actual speed
                                }

                                // Always after calculating all limits
                                double limitTicks = getLimitTicks(decimals, entityID);

                                if (decimals.has(limitTicks)) {
                                    limitTicks -= 1;

                                    if (limitTicks <= 0) {
                                        clearLimitCache(decimals, entityID);
                                    } else {
                                        decimals.set("ticks=" + entityID, limitTicks);
                                    }
                                }

                                if (horizontal >= limit) {
                                    messageCopy += ", extra: " + limit;
                                    cancelWithBuffer(p, vehicle, from, "horizontal", messageCopy, key, boatBufferTicks, Math.max(boatBufferFails - 1, 2));
                                }
                            } else {
                                clearLimitCache(decimals, entityID);
                            }
                        });
                        break;
                    case camelID:
                        //todo
                        break;
                    default:
                        clearLimitCache(decimals);
                        break;
                }
            } else {
                clearLimitCache(p.getDecimals(check));
                validity.remove(uuid);
            }
        } else {
            clearLimitCache(p.getDecimals(check));
            validity.remove(uuid);
        }
    }

    private static long canDo(SpartanPlayer p, SpartanLocation loc, Entity entity) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && p.canRunCheck(check, true)
                && !p.getHandlers().has(Handlers.HandlerType.Explosion) && !p.getHandlers().has(Handlers.HandlerType.ElytraUse)
                && !MyPet.isUsing(p)) {
            if (entity instanceof Boat) {
                UUID uuid = p.getUniqueId();

                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                    List<Entity> passengers = entity.getPassengers();

                    if (passengers.size() > 1) {
                        if (!passengers.get(0).getUniqueId().equals(uuid)) {
                            return 0;
                        }
                        for (Entity passenger : passengers) {
                            if (!(passenger instanceof Player) && !(passenger instanceof Animals) && !(passenger instanceof Monster)) {
                                return 0;
                            }
                        }
                    }
                }
                List<Entity> nearbyEntities = p.getNearbyEntities(0.25, 0, 0.25);

                if (nearbyEntities.size() > 0) {
                    for (Entity nearbyEntity : nearbyEntities) {
                        if (SpartanBukkit.isPlayer(nearbyEntity.getUniqueId())
                                && !nearbyEntity.equals(entity)) {
                            return 0;
                        }
                    }
                }
                SpartanLocation loc1 = loc.clone().add(0, 1, 0);

                if (!loc1.isBlock(Material.SLIME_BLOCK, 1)
                        && !loc1.isBlock(new Material[]{piston, sticky_piston, piston_extension, piston_moving}, 1)) {
                    return boatID;
                }
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                int finalID;

                if (entity instanceof Pig) {
                    finalID = pigID;
                } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)) {
                    if (entity instanceof Horse) {
                        finalID = horseID;
                    } else if (entity instanceof Llama) {
                        finalID = llamaID;
                    } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20) && entity instanceof Camel) {
                        finalID = camelID;
                    } else {
                        finalID = 0;
                    }
                } else {
                    finalID = 0;
                }

                if (finalID != 0) {
                    if (((LivingEntity) entity).hasPotionEffect(PotionEffectType.LEVITATION)) {
                        check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), 40);
                        return 0;
                    }
                    if (!p.getHandlers().has(Handlers.HandlerType.Piston)
                            && !((LivingEntity) entity).isLeashed()
                            && !Attributes.has(p, Attributes.HORSE_JUMP_STRENGTH)
                            && !BouncingBlocks.isSlimeBelow(p)) {
                        return finalID;
                    }
                }
            }
        }
        return 0;
    }

    // Utilities

    private static boolean cacheBoolean(SpartanPlayer p, boolean bool, String key, int ticks) {
        Cooldowns cooldowns = p.getCooldowns(check);
        boolean result;

        if (bool) {
            result = true;
            cooldowns.add(key, ticks);
        } else {
            result = !cooldowns.canDo(key);
        }
        return result;
    }

    private static List<SpartanLocation> getLiquidBlocks(SpartanLocation loc, double hitbox) {
        List<SpartanLocation> positives = new ArrayList<>(18); // 9 * 2

        for (int y = -1; y <= 0; y++) {
            SpartanLocation[] locations = loc.getSurroundingLocations(hitbox, y, hitbox);

            for (SpartanLocation location : locations) {
                if (location.getBlock().isNonWaterLoggedLiquid()) {
                    positives.add(loc);
                }
            }
        }
        return positives;
    }

    private static void cancel(SpartanPlayer p, Entity entity,
                               SpartanLocation loc,
                               String detection, String message) {
        boolean cancel = p.getProfile().isSuspectedOrHacker(check);
        new HackPrevention(p, check,
                "type: " + detection + "(" + entity.getType().toString().toLowerCase().replace("-", "") + "), " + message,
                cancel ? loc : null, cancel ? 20 : 0, true);
    }

    private static void cancelWithBuffer(SpartanPlayer p, Entity entity,
                                         SpartanLocation loc,
                                         String detection, String message, String key,
                                         int ticks, int fails) {
        Buffer buffer = p.getBuffer(check);
        key += detection;

        if (buffer.start(key, ticks) == fails) {
            buffer.remove(key);
            cancel(p, entity, loc, detection, message);
        }
    }

    private static double cacheLimit(Decimals decimals, int entityID, double existingLimit, double newLimit, int ticks) {
        if (newLimit > existingLimit) {
            existingLimit = newLimit;
        }
        if (ticks > 0) {
            double repetitiveLimit = decimals.get("repetitive-limit=" + entityID, 0.0);

            if (existingLimit >= repetitiveLimit) {
                if (existingLimit != repetitiveLimit) {
                    decimals.set("repetitive-limit=" + entityID, existingLimit);
                }
                if (ticks >= decimals.get("ticks=" + entityID, 0.0)) {
                    decimals.set("ticks=" + entityID, ticks);
                }
            }
        }
        return existingLimit;
    }

    private static double getGreaterRepetitiveLimit(Decimals decimals, int entityID) {
        return decimals.get("repetitive-limit=" + entityID, 0.0);
    }

    private static double getLimitTicks(Decimals decimals, int entityID) {
        return decimals.get("ticks=" + entityID);
    }

    private static void clearLimitCache(Decimals decimals, int entityID) {
        decimals.remove(new String[]{
                "repetitive-limit=" + entityID,
                "ticks=" + entityID
        });
    }

    private static void clearLimitCache(Decimals decimals) {
        for (int entityID : entityIDs) {
            clearLimitCache(decimals, entityID);
        }
    }
}
