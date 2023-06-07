package me.vagdedes.spartan.utils.gameplay;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.protections.Building;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.handlers.stability.Moderation;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class GroundUtils {

    private static final Set<Double>
            abstractHeights = new HashSet<>(),
            predictableHeights = new HashSet<>(),
            allHeights; // Attention
    private static final Set<Material> solidBlocks = new HashSet<>();
    private static final Map<Material, double[]> specificHeights = new LinkedHashMap<>();

    private static final double
            fallingMotion = MoveUtils.falling[0],
            jumpingMotion = MoveUtils.jumping[1],
            jumpingStartMotion = MoveUtils.jumping[0];
    public static final double
            hitbox = 0.305,
            bedStep = 0.5625,
            maxStep = bedStep + 0.1;
    public static final long lastJumpLegitimateDelay = 455L;

    public static final String
            inaccuracyKey = "ground-utils=utility-inaccuracy",
            vanillaGroundKey = "ground-utils=vanilla-on-ground";
    private static final String
            setOnGroundKey = "ground-utils=set-on-ground",
            setOffGroundKey = "ground-utils=set-off-ground",
            blockedJumpingKey = "ground-utils=blocked-jumping";

    private static final Material web = MaterialUtils.get("web");

    static {
        abstractHeights.add(maxStep);
        abstractHeights.add(bedStep);

        abstractHeights.add(0.1875);
        abstractHeights.add(0.8125);
        abstractHeights.add(0.9375);
        abstractHeights.add(0.375);
        abstractHeights.add(0.0625);
        abstractHeights.add(0.125);
        abstractHeights.add(0.4375);
        abstractHeights.add(0.875);
        abstractHeights.add(0.6875);
        abstractHeights.add(0.09375);
        abstractHeights.add(0.3125);
        abstractHeights.add(0.625);
        abstractHeights.add(0.015625);

        predictableHeights.add(0.0);
        predictableHeights.add(0.25);
        predictableHeights.add(0.75);
        predictableHeights.add(0.5);

        allHeights = new HashSet<>(abstractHeights.size() + predictableHeights.size());
        allHeights.addAll(abstractHeights);
        allHeights.addAll(predictableHeights);

        // Separator

        for (Material m : Material.values()) {
            if (BlockUtils.isSolid(m) || BlockUtils.areFenceGates(m)
                    || BlockUtils.areTrapdoors(m) || BlockUtils.isScaffoldingBlock(m)
                    || BlockUtils.isInteractiveAndPassable(m)) {
                solidBlocks.add(m);
            }

            // Separator

            if (BlockUtils.areSlabs(m) || BlockUtils.areStairs(m) || BlockUtils.areCobbleWalls(m) || BlockUtils.areDoors(m)
                    || BlockUtils.areFences(m) || BlockUtils.areFenceGates(m) || m == MaterialUtils.get("cake")
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && (m == Material.AZALEA || m == Material.FLOWERING_AZALEA)) {
                specificHeights.put(m, new double[]{0.0, 0.5});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && (m == Material.SCULK_SENSOR || m == Material.SCULK_SHRIEKER)) { //todo calibrated sensor
                specificHeights.put(m, new double[]{0.5});
            } else if (BlockUtils.areHeads(m)) {
                specificHeights.put(m, new double[]{0.0, 0.5, 0.75});
            } else if (BlockUtils.areTrapdoors(m)) {
                specificHeights.put(m, new double[]{0.0, 0.1875, 0.5});
            } else if (m == MaterialUtils.get("end_portal_frame")) {
                specificHeights.put(m, new double[]{0.0, 0.8125});
            } else if (m == MaterialUtils.get("enchanting_table")) {
                specificHeights.put(m, new double[]{0.75});
            } else if (m == MaterialUtils.get("soil") || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && m == Material.getMaterial("GRASS_PATH") || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.DIRT_PATH) {
                specificHeights.put(m, new double[]{0.9375});
            } else if (BlockUtils.areFlowerPots(m) || BlockUtils.areCandles(m)) {
                specificHeights.put(m, new double[]{0.0, 0.375});
            } else if (BlockUtils.areCarpets(m)) {
                specificHeights.put(m, new double[]{0.0625, 0.99999999});
            } else if (m == MaterialUtils.get("repeater_on") || m == MaterialUtils.get("repeater_off") || m == MaterialUtils.get("comparator_on") || m == MaterialUtils.get("comparator_off")) {
                specificHeights.put(m, new double[]{0.125});
            } else if (BlockUtils.areBeds(m)) {
                specificHeights.put(m, new double[]{bedStep});
            } else if (BlockUtils.areAnvils(m)) {
                specificHeights.put(m, new double[]{0.0, 0.25});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.MEDIUM_AMETHYST_BUD) {
                specificHeights.put(m, new double[]{0.0, 0.25, 0.8125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && (m == Material.CHORUS_PLANT || m == Material.CHORUS_FLOWER)) {
                specificHeights.put(m, new double[]{0.0, 0.8125});
            } else if (m == MaterialUtils.get("daylight_detector_1") || m == MaterialUtils.get("daylight_detector_2")) {
                specificHeights.put(m, new double[]{0.375});
            } else if (m == Material.SNOW
                    || BlockUtils.areShulkerBoxes(m)
                    || BlockUtils.isInteractiveAndPassable(m)
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && m == Material.END_ROD
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && m == Material.HONEY_BLOCK
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && m == Material.MUD) {
                specificHeights.put(m, new double[]{-1.0});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.STONECUTTER) {
                specificHeights.put(m, new double[]{0.0, 0.5, 0.5625, maxStep});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.LANTERN || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.SOUL_LANTERN) {
                specificHeights.put(m, new double[]{0.0, 0.5, 0.5625, 0.4375, 0.625, maxStep});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.CAMPFIRE || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.SOUL_CAMPFIRE) {
                specificHeights.put(m, new double[]{0.4375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.BELL) {
                specificHeights.put(m, new double[]{0.0, 0.9375, 0.8125, 0.375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.COMPOSTER) {
                specificHeights.put(m, new double[]{0.0, 0.125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.LECTERN) {
                specificHeights.put(m, new double[]{0.0, 0.125, 0.875});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.GRINDSTONE) {
                specificHeights.put(m, new double[]{0.0, 0.8125, 0.875, 0.6875, 0.625, 0.5625, 0.75, maxStep});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.TURTLE_EGG) {
                specificHeights.put(m, new double[]{0.0, 0.4375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.AMETHYST_CLUSTER) {
                specificHeights.put(m, new double[]{0.0, 0.4375, 0.8125});
            } else if (BlockUtils.areCandleCakes(m)) {
                specificHeights.put(m, new double[]{0.0, 0.5, 0.875});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.CONDUIT || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.POINTED_DRIPSTONE) {
                specificHeights.put(m, new double[]{0.0, 0.6875});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.CHAIN) {
                specificHeights.put(m, new double[]{0.0, 0.59375});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.LARGE_AMETHYST_BUD) {
                specificHeights.put(m, new double[]{0.0, 0.3125, 0.8125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.SMALL_AMETHYST_BUD) {
                specificHeights.put(m, new double[]{0.0, 0.1875, 0.75});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && (m == Material.POWDER_SNOW_CAULDRON || m == Material.WATER_CAULDRON || m == Material.LAVA_CAULDRON)) {
                specificHeights.put(m, new double[]{0.0, 0.25, 0.3125});
            } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.LILY_PAD || !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && m == Material.getMaterial("WATER_LILY")) {
                specificHeights.put(m, new double[]{0.09375, 0.015625});
            }
            switch (m) {
                case TRAPPED_CHEST:
                case CHEST:
                case ENDER_CHEST:
                    specificHeights.put(m, new double[]{0.875});
                case COCOA:
                    specificHeights.put(m, new double[]{0.75});
                case CAULDRON:
                    specificHeights.put(m, new double[]{0.0, 0.25, 0.3125});
                case BREWING_STAND:
                    specificHeights.put(m, new double[]{0.125, 0.875});
                case CACTUS:
                    specificHeights.put(m, new double[]{0.9375});
                case HOPPER:
                    specificHeights.put(m, new double[]{0.0, 0.6875});
                case SOUL_SAND:
                    specificHeights.put(m, new double[]{0.0, 0.875});
            }
        }
    }

    public static void clear(SpartanPlayer p) {
        p.getBuffer().remove(inaccuracyKey);
    }

    // Runnables

    public static void setOnGround(SpartanPlayer p, int ticks) {
        p.getCooldowns().add(setOnGroundKey, ticks);
        p.setAirTicks(0);
    }

    public static void setOffGround(SpartanPlayer p, int ticks) {
        p.getCooldowns().add(setOffGroundKey, ticks);
    }

    // Method

    public static boolean isOnGround(SpartanPlayer p, SpartanLocation loc, double y, boolean liquid, boolean climbable) {
        boolean player = p != null,
                original = y == 0.0,
                reset = player && original;
        double locY = loc.getY(),
                remaining = locY - loc.getBlockY(),
                nmsVerticalDistance = player ? p.getNmsVerticalDistance() : 0.0;
        Cooldowns cooldowns = player ? p.getCooldowns() : null;
        Decimals decimals = player ? p.getDecimals() : null;
        int airTicks = player ? p.getTicksOnAir() : Integer.MAX_VALUE;

        if (player) {
            if (!cooldowns.canDo(setOffGroundKey)) {
                return false;
            }
            if (!cooldowns.canDo(setOnGroundKey)) {
                if (original) {
                    p.setAirTicks(0);
                }
                return true;
            }
            if (original) {
                if (p.getVehicle() != null && allHeights.contains(remaining)) { // When a vehicle is used, the player's object location changes partly to the vehicle. Due to some vehicles having larger areas of collision, we allow them to be checked by heights purely.
                    return true;
                }
                if (isBlockedJumping(p, loc, cooldowns, nmsVerticalDistance, remaining, airTicks)) {
                    p.setAirTicks(0);
                    setOnGround(p, 3); // 3 specifically for 0.2, 0.1216 & 0.0784
                    return true;
                }
            }
        }
        boolean onGroundVanilla = !player || p.isOnGround(),
                boats = false;

        if (player
                && ((boats = stepsOnBoats(p, cooldowns, original ? loc : p.getLocation()))

                || original
                && (onGroundVanilla || airTicks < 20 && nmsVerticalDistance > 0.0 && Math.abs(nmsVerticalDistance - jumpingMotion) < MoveUtils.jumping[2] && p.getOldLastJump() > lastJumpLegitimateDelay)
                // airTicks < 20 && nmsVerticalDistance > -0.72 && MoveUtils.getLastJump(p) > 355L && MoveUtils.isFalling(nmsVerticalDistance)
                && vanillaGroundCorrection(loc, decimals, airTicks, nmsVerticalDistance)

                || stepsOnShulkers(p)
                || stepsOnSniffers(p)
                || p.getHandlers().has(Handlers.HandlerType.Piston))

                || onGroundVanilla && isCobblestoneWall(p, loc, remaining)) {
            if (reset && !boats) {
                p.setAirTicks(0);
            }
            return true;
        }

        for (SpartanLocation locs : getLocations(
                p,
                loc,
                player ? p.getVehicle() : null,
                player ? Math.max(getCalculatedFallingDistance(p), fallingMotion) : fallingMotion,
                remaining,
                nmsVerticalDistance)) {
            if (!original || Math.abs(locY - locs.getY()) <= 1.5) {
                SpartanBlock block = locs.getBlock();
                Material m = block.getType();

                if (liquid && block.isLiquid()
                        || climbable && (BlockUtils.canClimb(m) || m == web)
                        || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && m == Material.HONEY_BLOCK) {
                    if (reset) {
                        p.setAirTicks(0);
                    }
                    return true;
                }
                if (reset && nmsVerticalDistance <= 0.0) {
                    if (nmsVerticalDistance > -0.1) {
                        if (locs.clone().add(0, 1, 0).getBlock().getType() == web
                                || locs.clone().add(0, 2, 0).getBlock().getType() == web) {
                            p.setAirTicks(0);
                            return true;
                        }
                    } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && nmsVerticalDistance > -0.128) {
                        if (locs.clone().add(0, 1, 0).getBlock().getType() == Material.HONEY_BLOCK
                                || locs.clone().add(0, 2, 0).getBlock().getType() == Material.HONEY_BLOCK) {
                            p.setAirTicks(0);
                            return true;
                        }
                    }
                }
                if (solidBlocks.contains(m)) {
                    if (!original && airTicks <= 12 || player && p.isBedrockPlayer()) { // Extra Location
                        return true;
                    }

                    for (double heights : specificHeights.getOrDefault(m, new double[]{0.0})) {
                        if (remaining == heights
                                || heights == -1.0
                                || heights > 0.0 && (remaining % heights) == 0.0
                                || heights == 0.0 && isZeroMiscalculation(remaining)

                                || predictableHeights.contains(heights)
                                && (Math.abs(jumpingMotion - nmsVerticalDistance) < MoveUtils.jumping[2]
                                || Math.abs(jumpingMotion - remaining) < MoveUtils.jumping[2])) {
                            if (reset) {
                                p.setAirTicks(0);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Base

    private static List<SpartanLocation> getLocations(SpartanPlayer p, SpartanLocation loc, Entity vehicle,
                                                      double motion, double remaining, double vertical) {
        if (p == null) {
            p = loc.getPlayer();
        }
        // Protection
        motion = Math.max(motion, 1.0);

        // Array
        List<SpartanLocation> array = new ArrayList<>(10);

        // Coordinates
        double correctY = 0.0;

        if (isZeroMiscalculation(remaining) || vertical > 0.0 && vertical < MoveUtils.lowPrecision || p != null && p.isFalling(vertical)) {
            correctY = -motion;
        } else if (remaining == 0.5) {
            array.add(loc.clone().add(0, -(remaining + motion), 0));
        }
        double box = heightExists(remaining, false) ? GroundUtils.hitbox : BlockUtils.hitbox_max;

        if (vehicle != null) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                BoundingBox boundingBox = vehicle.getBoundingBox();
                box = Math.max(
                        Math.max(
                                boundingBox.getMaxX() - boundingBox.getMinX(),
                                boundingBox.getMaxZ() - boundingBox.getMinZ()
                        ),
                        box
                );
            } else if (vehicle instanceof LivingEntity) {
                box = Math.max(CombatUtils.getWidthAndHeight((LivingEntity) vehicle)[0], box);
            }
        }
        array.addAll(loc.getSurroundingLocations(box, correctY, box, true));
        return array;
    }

    public static double getHeight(Material m) {
        double max = 0.0;

        for (double height : specificHeights.getOrDefault(m, new double[]{0.0})) {
            max = Math.max(max, height);
        }
        return max;
    }

    // Utils

    public static boolean heightExists(double d, boolean all) {
        return abstractHeights.contains(d) || all && predictableHeights.contains(d);
    }

    public static boolean stepsOnBoats(SpartanPlayer p, Cooldowns cooldowns, SpartanLocation to) {
        double hitbox = (to.getY() - MoveUtils.getCachedLocation(p).getY()) < 0.0
                || BlockUtils.isLiquid(to)
                || BlockUtils.isLiquid(to.clone().add(0, -1, 0)) ? 2.5 : 1.5; // 2.5 is extra

        for (Entity entity : p.getNearbyEntities(hitbox, hitbox, hitbox)) {
            if (entity instanceof Boat) {
                p.setAirTicks(0);
                cooldowns.add("ground-utils=boat-stepping", 4);
                return true;
            }
        }
        return !cooldowns.canDo("ground-utils=boat-stepping");
    }

    public static boolean stepsOnShulkers(SpartanPlayer p) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            for (Entity entity : p.getNearbyEntities(3.0, 3.0, 3.0)) {
                if (entity instanceof Shulker) {
                    p.setAirTicks(0);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean stepsOnSniffers(SpartanPlayer p) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)) {
            for (Entity entity : p.getNearbyEntities(1.0, 1.0, 1.0)) {
                if (entity instanceof Sniffer && ((Sniffer) entity).getState() == Sniffer.State.IDLING) {
                    p.setAirTicks(0);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSolidAndLiquidFree(SpartanLocation loc, double y) {
        for (SpartanLocation locs : loc.getSurroundingLocations(BlockUtils.hitbox_safe, y, BlockUtils.hitbox_safe)) {
            if (BlockUtils.isSolid(locs) || BlockUtils.isLiquid(locs)) {
                return false;
            }
        }
        return true;
    }

    private static double getCalculatedFallingDistance(SpartanPlayer p) {
        double vanilla = p.getNmsVerticalDistance();

        if (p.getDecimals().has(vanilla) && vanilla < 0.0) {
            vanilla -= fallingMotion;
            double calculated = fallingMotion * (p.getTicksOnAir() + 1);
            return Math.abs(calculated - Math.abs(vanilla)) <= 1.0 ? vanilla : calculated;
        }
        return 0.0;
    }

    // Patches

    public static boolean isZeroMiscalculation(double dbl) {
        return dbl < 0.0001;
    }

    private static boolean isBlockedJumping(SpartanPlayer p, SpartanLocation to, Cooldowns cooldowns, double vertical, double remaining, int airTicks) {
        boolean hasCooldown = cooldowns != null;

        if (airTicks <= 12
                && p.getLastJump() >= 1_000L
                && (isZeroMiscalculation(vertical - 0.2) || isZeroMiscalculation(remaining - 0.2))
                && !BlockUtils.isSolidFree(to, BlockUtils.hitbox, 2, BlockUtils.hitbox)) {
            vertical = Math.abs(vertical - remaining);

            for (double height : allHeights) {
                if (isZeroMiscalculation(vertical - height)) {
                    if (hasCooldown) {
                        cooldowns.add(blockedJumpingKey, 5);
                    }
                    return true;
                }
            }
        }
        return hasCooldown && !cooldowns.canDo(blockedJumpingKey);
    }

    public static boolean isBlockedJumping(SpartanPlayer p, SpartanLocation to, double vertical, double remaining, int airTicks) {
        return isBlockedJumping(p, to, p.getCooldowns(), vertical, remaining, airTicks);
    }

    public static void runOnJumpMovement(SpartanPlayer p, SpartanLocation loc, int airTicks) { // Attention, changed recently
        if (airTicks > 11 && !p.getProfile().isSuspectedOrHacker() && p.getCooldowns().canDo("ground-utils=set-off-ground")) {
            for (int i = -2; i <= 0; i++) {
                if (!BlockUtils.isSolidFree(loc, 1, i, 1)) {
                    setOnGround(p, 1);
                    break;
                }
            }
        }
    }

    private static boolean isCobblestoneWall(SpartanPlayer p, SpartanLocation loc, double remaining) {
        if (remaining == 0.5) {
            double dx = loc.getX() - loc.getBlockX();

            if (dx < 0.3 || dx > 0.7) {
                return !solidBlocks.contains(loc.getBlock().getType()) && (p == null || BlockUtils.areWallsAround(loc, BlockUtils.hitbox, -0.6, BlockUtils.hitbox));
            }
            double dz = loc.getZ() - loc.getBlockZ();

            if (dz < 0.3 || dz > 0.7) {
                return !solidBlocks.contains(loc.getBlock().getType()) && (p == null || BlockUtils.areWallsAround(loc, BlockUtils.hitbox, -0.6, BlockUtils.hitbox));
            }
        }
        return false;
    }

    private static boolean vanillaGroundCorrection(SpartanLocation loc, Decimals decimals, int airTicks, double ver) {
        if (decimals.get(vanillaGroundKey, 0.0) == 0.0) {
            if (airTicks > 4 && airTicks < 20) {
                return !isSolidAndLiquidFree(loc, -0.9);
            }
            if (ver > -0.122) {
                return !isSolidAndLiquidFree(loc, -0.9);
            }
            if (ver < 0.0) {
                ver = 0.0 - ver; // Math.abs() but straight
                return ((ver % fallingMotion) <= fallingMotion || (ver % MoveUtils.falling[1]) <= fallingMotion) && !isSolidAndLiquidFree(loc, -0.9);
            }
        }
        return false;
    }

    public static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from, boolean ground) {
        p.getDecimals().set(GroundUtils.vanillaGroundKey, p.isOnGround() ? 1 : 0);

        // Separator

        int blockY = to.getBlockY();

        if ((blockY + 0.5) >= from.getBlockY() && to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ()
                && !LagLeniencies.hasInconsistencies(p, "tps") && !LagLeniencies.hasHighLatency(p, LagLeniencies.badPing) // Base
                && !Building.hasCooldown(p)
                && !BlockUtils.canClimb(to) && !BlockUtils.canClimb(from)
                && !BlockUtils.isSolidFree(to, hitbox, 0, hitbox)) {
            if ((!ground || !p.isOnGround())
                    && (to.getY() - blockY) != 0.5
                    && !Moderation.wasDetected(p)
                    && p.getNmsVerticalDistance() > jumpingStartMotion
                    && p.getBuffer().increase(inaccuracyKey, 1) >= 12) {
                setOffGround(p, 1);
            }
        } else {
            p.getBuffer().remove(inaccuracyKey);
        }
    }
}
