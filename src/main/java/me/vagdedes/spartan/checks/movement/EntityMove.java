package me.vagdedes.spartan.checks.movement;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.compatibility.manual.entity.CraftBook;
import me.vagdedes.spartan.compatibility.manual.entity.MyPet;
import me.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.handlers.identifiers.complex.unpredictable.Damage;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.*;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntityMove extends CheckExecutor {

    public static final Enums.HackType check = Enums.HackType.EntityMove;

    private static final int liquidCacheTicks = 30, iceCacheTicks = 30,
            horseID = 1, boatID = 2, pigID = 3, llamaID = 4, camelID = 5,
            horseJumpPositiveMotions = 5,
            livingEntityBufferTicks = 20, livingEntityBufferFails = 3,
            boatBufferTicks = 20, boatBufferFails = 3;

    private static final double horseMinimumHorizontal = 0.5,
            pigMinimumHorizontal = 0.15, pigCarrotMinimumHorizontal = 0.3,
            boatMotionPrecision = 0.001;

    private Long[] cache = null;
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

    public EntityMove(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean handle(Object object) {
        return false;
    }

    @Override
    public boolean run() {
        Entity vehicle = player.getVehicle();

        if (vehicle != null) {
            SpartanLocation to = player.getEventToLocation();
            SpartanLocation from = player.getEventFromLocation();
            long entityLongID = getAllowance();

            if (entityLongID != 0) {
                double horizontal = player.getNmsHorizontalDistance(),
                        vertical = player.getNmsVerticalDistance(),
                        oldVertical = player.getOld_NmsVerticalDistance(),
                        box = player.getNmsBox(),
                        oldBox = player.getOld_NmsBox();
                int airTicks = player.getTicksOnAir();

                if (cache == null) {
                    cache = new Long[]{entityLongID, System.currentTimeMillis()};
                    return false;
                } else if (cache[0] != entityLongID) {
                    cache = new Long[]{entityLongID, System.currentTimeMillis()};
                    hackType.getCheck().addDisabledUser(player.getUniqueId(), hackType.toString(), 4);
                    return false;
                }
                int entityID = (int) entityLongID;
                long time = System.currentTimeMillis() - cache[1];
                List<SpartanLocation> liquidBlocks = getLiquidBlocks(
                        to,
                        Math.max(CombatUtils.getWidthAndHeight(vehicle)[0], BlockUtils.hitbox)
                );
                boolean unstableLiquids = cacheBoolean(player, false, "unstable-liquids", liquidCacheTicks),
                        liquidsLocal = !liquidBlocks.isEmpty(),
                        liquids = cacheBoolean(player, liquidsLocal, "liquids", liquidCacheTicks) || player.wasInLiquids(),
                        iceBlocks = cacheBoolean(player,
                                PatternUtils.isBlockPattern(ice, to, true, BlockUtils.ice),
                                "ice-blocks", iceCacheTicks),
                        damage = Damage.hasCooldown(player) || player.getHandlers().has(Handlers.HandlerType.Explosion),
                        knownBoxes = GroundUtils.heightExists(box),
                        oldKnownBoxes = GroundUtils.heightExists(oldBox),
                        checkVertical = hackType.getCheck().getBooleanOption("check_vertical", true),
                        checkHorizontal = hackType.getCheck().getBooleanOption("check_horizontal", true);
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
                Decimals decimals = player.getDecimals(hackType);

                if (liquidsLocal) {
                    for (SpartanLocation liquid : liquidBlocks) {
                        if (!BlockUtils.isClearLiquid(null, liquid)) {
                            unstableLiquids = cacheBoolean(player, true, "unstable-liquids", liquidCacheTicks);
                            break;
                        }
                    }
                }

                switch (entityID) {
                    case horseID:
                    case llamaID:
                        runAsync(() -> {
                            clearLimitCache(player.getDecimals(hackType)); // Attention

                            if (!Damage.hasCooldown(player)) {
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
                                                cancel(vehicle, from, "rapid-upwards", messageCopy);
                                            }
                                        }
                                    } else if (!liquids) {
                                        if (vertical > verticalLimit) {
                                            messageCopy += ", extra: " + verticalLimit;
                                            cancel(vehicle, from, "abstract-upwards", messageCopy);
                                        } else if (vertical > oldVertical) {
                                            if (oldVertical < 0.0) {
                                                if (!oldKnownBoxes
                                                        && !PatternUtils.isBlockPattern(web, to, true, BlockUtils.web)) {
                                                    cancel(vehicle, from, "illegal-upwards", messageCopy);
                                                }
                                            } else {
                                                cancelWithBuffer(player, vehicle, from, "irregular-upwards", messageCopy, key, livingEntityBufferTicks, livingEntityBufferFails);
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
                                            cancel(vehicle, from, "horizontal", messageCopy);
                                        }
                                    }
                                }
                            }
                        });
                        break;
                    case pigID:
                        runAsync(() -> {
                            clearLimitCache(player.getDecimals(hackType)); // Attention

                            if (!Damage.hasCooldown(player)) {
                                String messageCopy = message;
                                boolean notStepping = (!knownBoxes
                                        || !oldKnownBoxes
                                        || !PatternUtils.isBlockPattern(pig, to, true, BlockUtils.solid, BlockUtils.semi_solid));

                                if (checkVertical) {
                                    if (notStepping && vertical >= (damage ? 0.5 : 0.1)) {
                                        cancel(vehicle, from, "rapid-upwards", messageCopy);
                                    } else if (vertical > 0.0 && airTicks >= (damage ? 20 : 12)) {
                                        cancel(vehicle, from, "abstract-upwards", messageCopy);
                                    } else if (notStepping && verticalDifference >= (damage ? 0.5 : 0.2)) {
                                        cancel(vehicle, from, "irregular-upwards", messageCopy);
                                    } else if (vertical >= oldVertical && vertical != 0.0 && oldVertical != 0.0
                                            && !PatternUtils.isBlockPattern(web, to, true, BlockUtils.web)) {
                                        cancel(vehicle, from, "illegal-upwards", messageCopy);
                                    }
                                }
                                if (checkHorizontal) {
                                    double speedLimit;

                                    if (player.getItemInHand().getType() == carrotOnAStick
                                            || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && player.getInventory().getItemInOffHand().getType() == carrotOnAStick) {
                                        speedLimit = pigCarrotMinimumHorizontal;
                                    } else {
                                        speedLimit = pigMinimumHorizontal;
                                    }

                                    if (horizontal >= speedLimit) {
                                        messageCopy += ", extra: " + speedLimit;
                                        cancel(vehicle, from, "horizontal", messageCopy);
                                    }
                                }
                            }
                        });
                        break;
                    case boatID:
                        boolean unstableLiquidsCopy = unstableLiquids;

                        runAsync(() -> {
                            String messageCopy = message;
                            boolean offGround = airTicks > 0 && !player.isOnGroundCustom()
                                    && (!knownBoxes && !oldKnownBoxes || !PlayerData.isOnGround(player, to, 0, -1, 0));

                            if (checkVertical && !BouncingBlocks.isBelow(player)) {
                                if (vertical > boatMotionPrecision) { // Upwards
                                    if (offGround && !player.getHandlers().has(Handlers.HandlerType.WaterElevator)) {
                                        if (vertical > oldVertical) { // Elevation
                                            if (!unstableLiquidsCopy) {
                                                cancelWithBuffer(player, vehicle, from, "elevation", messageCopy, key, boatBufferTicks, boatBufferFails);
                                            }
                                        } else if (vertical < oldVertical) { // Slower Elevation
                                            cancelWithBuffer(player, vehicle, from, "slower-elevation", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        } else { // Steady Elevation
                                            cancelWithBuffer(player, vehicle, from, "steady-elevation", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        }
                                    }
                                } else if (vertical < -boatMotionPrecision) { // Downwards
                                    if (vertical < oldVertical) { // Fall
                                        double divisor = 0.04;

                                        if (!liquids && Math.abs(Math.abs(vertical % divisor) - divisor) >= boatMotionPrecision) {
                                            cancelWithBuffer(player, vehicle, from, "modulo", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        } else if (offGround) {
                                            double difference = oldVertical - vertical;

                                            if (difference > (divisor + boatMotionPrecision)) {
                                                messageCopy += ", extra: " + difference;
                                                cancelWithBuffer(player, vehicle, from, "difference", messageCopy, key, boatBufferTicks, boatBufferFails);
                                            }
                                        }
                                    } else if ((vertical - oldVertical) > boatMotionPrecision) { // Slower Fall
                                        if (!liquids && offGround && !player.getHandlers().has(Handlers.HandlerType.WaterElevator)) {
                                            cancelWithBuffer(player, vehicle, from, "slow-fall", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        }
                                    } else { // Steady Fall
                                        if (!liquids && !player.getHandlers().has(Handlers.HandlerType.WaterElevator)) {
                                            cancelWithBuffer(player, vehicle, from, "steady-fall", messageCopy, key, boatBufferTicks, boatBufferFails);
                                        }
                                    }
                                } else { // Neutral
                                    if (!liquids && offGround) {
                                        cancelWithBuffer(player, vehicle, from, "neutral", messageCopy, key, boatBufferTicks, boatBufferFails * 2);
                                    }
                                }
                            }
                            if (checkHorizontal) {
                                double limit = getGreaterRepetitiveLimit(decimals, entityID) + CraftBook.getBoatLimit();

                                if (iceBlocks) {
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
                                    cancelWithBuffer(player, vehicle, from, "horizontal", messageCopy, key, boatBufferTicks, Math.max(boatBufferFails - 1, 2));
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
                clearLimitCache(player.getDecimals(hackType));
                cache = null;
            }
        } else {
            clearLimitCache(player.getDecimals(hackType));
            cache = null;
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                && player.canRunCheck(hackType, true)
                && !player.getHandlers().has(Handlers.HandlerType.Explosion)
                && !player.getHandlers().has(Handlers.HandlerType.ElytraUse)
                && !MyPet.isUsing(player);
    }

    private long getAllowance() {
        if (canDo()) {
            Entity entity = player.getVehicle();

            if (entity instanceof Boat) {
                UUID uuid = player.getUniqueId();

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
                List<Entity> nearbyEntities = player.getNearbyEntities(0.25, 0, 0.25);

                if (!nearbyEntities.isEmpty()) {
                    for (Entity nearbyEntity : nearbyEntities) {
                        if (SpartanBukkit.isPlayer(nearbyEntity.getUniqueId())
                                && !nearbyEntity.equals(entity)) {
                            return 0;
                        }
                    }
                }
                SpartanLocation loc1 = player.getEventToLocation().clone().add(0, 1, 0);

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
                        hackType.getCheck().addDisabledUser(player.getUniqueId(), hackType.toString(), 40);
                        return 0;
                    }
                    if (!player.getHandlers().has(Handlers.HandlerType.Piston)
                            && !((LivingEntity) entity).isLeashed()
                            && !Attributes.has(player, Attributes.HORSE_JUMP_STRENGTH)
                            && !BouncingBlocks.isSlimeBelow(player)) {
                        return finalID;
                    }
                }
            }
        }
        return 0;
    }

    // Utilities

    private boolean cacheBoolean(SpartanPlayer p, boolean bool, String key, int ticks) {
        Cooldowns cooldowns = p.getCooldowns(hackType);
        boolean result;

        if (bool) {
            result = true;
            cooldowns.add(key, ticks);
        } else {
            result = !cooldowns.canDo(key);
        }
        return result;
    }

    private List<SpartanLocation> getLiquidBlocks(SpartanLocation loc, double hitbox) {
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

    private void cancel(Entity entity,
                        SpartanLocation loc,
                        String detection, String message) {
        boolean cancel = player.getProfile().isSuspectedOrHacker(hackType);
        cancel("type: " + detection + "(" + entity.getType().toString().toLowerCase().replace("-", "") + "), " + message,
                cancel ? loc : null, cancel ? 20 : 0, true);
    }

    private void cancelWithBuffer(SpartanPlayer p, Entity entity,
                                  SpartanLocation loc,
                                  String detection, String message, String key,
                                  int ticks, int fails) {
        Buffer buffer = p.getBuffer(hackType);
        key += detection;

        if (buffer.start(key, ticks) == fails) {
            buffer.remove(key);
            cancel(entity, loc, detection, message);
        }
    }

    private double cacheLimit(Decimals decimals, int entityID, double existingLimit, double newLimit, int ticks) {
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

    private double getGreaterRepetitiveLimit(Decimals decimals, int entityID) {
        return decimals.get("repetitive-limit=" + entityID, 0.0);
    }

    private double getLimitTicks(Decimals decimals, int entityID) {
        return decimals.get("ticks=" + entityID);
    }

    private void clearLimitCache(Decimals decimals, int entityID) {
        decimals.remove(new String[]{
                "repetitive-limit=" + entityID,
                "ticks=" + entityID
        });
    }

    private void clearLimitCache(Decimals decimals) {
        for (int entityID : entityIDs) {
            clearLimitCache(decimals, entityID);
        }
    }
}
