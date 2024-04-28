package com.vagdedes.spartan.utils.gameplay;

import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPotionEffect;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.util.*;

import static org.bukkit.potion.PotionEffectType.*;

public class PlayerUtils {

    private static final Material
            gold_axe = MaterialUtils.get("gold_axe"),
            wood_axe = MaterialUtils.get("wood_axe"),
            gold_pickaxe = MaterialUtils.get("gold_pickaxe"),
            wood_pickaxe = MaterialUtils.get("wood_pickaxe"),
            diamond_spade = MaterialUtils.get("diamond_spade"),
            iron_spade = MaterialUtils.get("iron_spade"),
            gold_spade = MaterialUtils.get("gold_spade"),
            stone_spade = MaterialUtils.get("stone_spade"),
            wood_spade = MaterialUtils.get("wood_spade");

    private static final double
            climbingMin = 0.03684,
            climbingMax = 0.15444,
            climbingDefault = 0.11760;
    public static final double
            gravityAcceleration = AlgebraUtils.floatDouble(0.0784),
            terminalVelocity = 3.92,
            drag = gravityAcceleration / terminalVelocity,
            dragComplement = 1.0 - drag,
            highPrecision = 0.0019,
            lowPrecision = highPrecision * 2.0,
            chunk = 16.0,
            climbingScaffoldingMax = climbingDefault + 0.2,
            maxJumpingMotionDifference;

    public static final double[]
            climbing = new double[]{climbingMin, 0.07531, 0.11215, climbingDefault, 0.12529, 0.15, climbingMax},
            climbingMinMax = new double[]{climbingMin, climbingMax};

    public static final int
            height,
            fallDamageBlocks = 4;

    private static final Map<Integer, Double>
            jumpsValues = new LinkedHashMap<>(),
            gravityValues = new LinkedHashMap<>(),
            gravityValuesAfterJump = new LinkedHashMap<>();
    private static final Map<Double, Integer>
            jumpTicks = new LinkedHashMap<>(),
            gravityTicks = new LinkedHashMap<>(),
            gravityTicksAfterJump = new LinkedHashMap<>();
    private static final Map<PotionEffectType, Integer> handledPotionEffects = new LinkedHashMap<>();

    static {
        handledPotionEffects.put(JUMP, 100);
        handledPotionEffects.put(SPEED, 60);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            handledPotionEffects.put(DOLPHINS_GRACE, 30);
            handledPotionEffects.put(SLOW_FALLING, 20);
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            handledPotionEffects.put(LEVITATION, 10);
        }

        // Separator
        int counter = 0;
        double jump = AlgebraUtils.floatDouble(0.42);

        while (true) {
            if (jump > 0.0) {
                double value = AlgebraUtils.cut(jump, GroundUtils.maxHeightLength);
                jumpsValues.put(counter, value);
                jumpTicks.put(value, counter);
                jump = (jump * dragComplement) - gravityAcceleration;
                counter++;
            } else {
                break;
            }
        }
        Iterator<Double> iterator = jumpsValues.values().iterator();
        double maxJumpingMotionDifferenceLocal = Double.MIN_VALUE,
                previousJumpingMotion = iterator.next();

        while (iterator.hasNext()) {
            double currentMotion = iterator.next();
            maxJumpingMotionDifferenceLocal = Math.max(
                    previousJumpingMotion - currentMotion,
                    maxJumpingMotionDifferenceLocal
            );
            previousJumpingMotion = currentMotion;
        }
        maxJumpingMotionDifference = maxJumpingMotionDifferenceLocal;

        // Separator

        double fall = 0.0,
                fallAfterJump = -jumpsValues.get(counter - 1);
        counter = 0;

        while (true) {
            fall = (fall * dragComplement) + gravityAcceleration;
            fallAfterJump = (fallAfterJump * dragComplement) + gravityAcceleration;
            boolean end = false;

            if ((terminalVelocity - fall) > highPrecision) {
                double value = AlgebraUtils.cut(fall, GroundUtils.maxHeightLength);
                gravityValues.put(counter, value);
                gravityTicks.put(value, counter);
            } else {
                end = true;
            }
            if ((terminalVelocity - fallAfterJump) > highPrecision) {
                double valueAfterJump = AlgebraUtils.cut(fallAfterJump, GroundUtils.maxHeightLength);
                gravityValuesAfterJump.put(counter, valueAfterJump);
                gravityTicksAfterJump.put(valueAfterJump, counter);
            } else if (end) {
                break;
            }
            counter++;
        }

        // Separator

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            List<World> worlds = Bukkit.getWorlds();

            if (!worlds.isEmpty()) {
                int max = 256;

                for (World world : worlds) {
                    max = Math.max(world.getMaxHeight(), max);
                }
                height = max;
            } else {
                height = 256;
            }
        } else {
            height = 256;
        }
    }

    // Enchantments

    public static int getDepthStriderLevel(SpartanPlayer p) {
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            return 0;
        }
        ItemStack b = p.getInventory().getBoots();
        return b != null ? b.getEnchantmentLevel(Enchantment.DEPTH_STRIDER) : 0;
    }

    // Speed

    public static boolean hasSoulSpeedEnchantment(SpartanPlayer p, SpartanLocation location) {
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
            return false;
        }
        String key = "player-data=soul-speed-enchantment";
        Cooldowns cooldowns = p.getCooldowns();
        boolean b = false;

        if (!cooldowns.canDo(key)) {
            if (cooldowns.get(key) > 5) {
                return true;
            } else {
                b = true;
            }
        }
        if (location != null) {
            ItemStack boots = p.getInventory().getBoots();

            if (boots != null && boots.containsEnchantment(Enchantment.SOUL_SPEED)) {
                SpartanLocation locationM1;

                if (location.isBlock(Material.SOUL_SAND, BlockUtils.hitbox)
                        || location.isBlock(Material.SOUL_SOIL, BlockUtils.hitbox)
                        || (locationM1 = location.clone().add(0, -1, 0)).isBlock(Material.SOUL_SAND, BlockUtils.hitbox)
                        || locationM1.isBlock(Material.SOUL_SOIL, BlockUtils.hitbox)) {
                    b = true;
                }
            }

            if (b) {
                p.getCooldowns().add(key, 20);
            }
        }
        return b;
    }

    public static boolean hasSpeedEffect(SpartanPlayer p, SpartanLocation location, boolean soulSpeed) {
        return p.getVehicle() == null
                && (p.hasPotionEffect(PotionEffectType.SPEED, getPotionEffectExtraTime(PotionEffectType.SPEED))
                || soulSpeed && hasSoulSpeedEnchantment(p, location));
    }

    // Jumping

    public static boolean hasJumpEffect(SpartanPlayer p) {
        return p.hasPotionEffect(JUMP, getPotionEffectExtraTime(JUMP));
    }

    public static boolean hasLowJumpEffect(SpartanPlayer p) {
        if (p.getVehicle() != null) {
            return false;
        }
        int potionLevel = getPotionLevel(p, JUMP);
        return potionLevel > 0 && potionLevel <= 128
                || potionLevel >= 250;
    }

    public static boolean hasHighJumpEffect(SpartanPlayer p) {
        if (p.getVehicle() != null) {
            return false;
        }
        int potionLevel = getPotionLevel(p, JUMP);
        return potionLevel > 128
                && potionLevel < 250;
    }

    public static double getJumpingPrecision(SpartanPlayer p, double def) {
        return p.bedrockPlayer || hasJumpEffect(p) ? highPrecision : def;
    }

    public static double getJumpingPrecision(SpartanPlayer p) {
        return getJumpingPrecision(p, 0.0001);
    }

    public static boolean isJumping(double d, double diff) {
        if (d > 0.0
                && !GroundUtils.blockHeightExists(d)
                && !GroundUtils.collisionHeightExists(d)) {
            if (diff == 0.0) {
                return jumpTicks.containsKey(AlgebraUtils.cut(d, GroundUtils.maxHeightLength));
            } else {
                for (double value : jumpsValues.values()) {
                    if (Math.abs(value - d) < diff) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static double getJumpMotion(int tick) {
        return jumpsValues.get(tick);
    }

    public static Collection<Double> getJumpMotions() {
        return jumpsValues.values();
    }

    public static int getJumpTicks() {
        return jumpsValues.size();
    }

    // Falling

    public static int getFallTick(double d) {
        if (d < 0.0) {
            d = AlgebraUtils.cut((0.0 - d), GroundUtils.maxHeightLength);
            return gravityTicks.getOrDefault(
                    d,
                    gravityTicksAfterJump.getOrDefault(d, -1)
            );
        } else {
            return -1;
        }
    }

    public static double getFallMotion(int tick) {
        return gravityValues.getOrDefault(
                tick,
                gravityValuesAfterJump.getOrDefault(tick, -1.0)
        );
    }

    public static boolean isFalling(double d) {
        return getFallTick(d) != -1;
    }

    public static Collection<Double> getFallMotions() {
        return gravityValues.values();
    }

    public static Collection<Double> getFallAfterJumpMotions() {
        return gravityValuesAfterJump.values();
    }

    public static double getSignificantFallTicks() {
        return gravityValues.size();
    }

    // Handled Effects

    public static boolean isInWaterTunnel(SpartanPlayer p, SpartanLocation location) {
        return p.movement.wasInLiquids()
                && hasDolphinsGraceEffect(p)
                && hasSoulSpeedEnchantment(p, location);
    }

    public static boolean hasDolphinsGraceEffect(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                && p.getVehicle() == null
                && p.hasPotionEffect(
                PotionEffectType.DOLPHINS_GRACE,
                getPotionEffectExtraTime(PotionEffectType.DOLPHINS_GRACE)
        );
    }

    public static boolean hasSlowFallingEffect(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                && p.getVehicle() == null
                && p.hasPotionEffect(
                PotionEffectType.SLOW_FALLING,
                getPotionEffectExtraTime(PotionEffectType.SLOW_FALLING)
        );
    }

    public static boolean hasLevitationEffect(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                && p.hasPotionEffect(
                PotionEffectType.LEVITATION,
                PlayerUtils.getPotionEffectExtraTime(PotionEffectType.LEVITATION)
        );
    }

    // Inventory

    public static boolean isUsable(Material type) {
        return CombatUtils.isSword(type)
                || CombatUtils.isBow(type)
                || type.isEdible()
                || type == Material.FISHING_ROD
                || type == Material.POTION;
    }

    public static boolean isSpadeItem(Material m) {
        return m == diamond_spade
                || m == iron_spade
                || m == gold_spade
                || m == stone_spade
                || m == wood_spade
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.NETHERITE_HOE;
    }

    public static boolean isPickaxeItem(Material m) {
        return m == Material.DIAMOND_PICKAXE
                || m == Material.IRON_PICKAXE
                || m == Material.STONE_PICKAXE
                || m == gold_pickaxe
                || m == wood_pickaxe
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.NETHERITE_PICKAXE;
    }

    public static boolean isAxeItem(Material m) {
        return m == Material.DIAMOND_AXE
                || m == Material.IRON_AXE
                || m == Material.STONE_AXE
                || m == gold_axe
                || m == wood_axe
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.NETHERITE_AXE;
    }

    // Movement

    public static double calculateLimit(SpartanPlayer entity, SpartanLocation location, double value, double divide, PotionEffectType potionEffectType) {
        int level;

        if (potionEffectType == PotionEffectType.SPEED) {
            level = getPotionLevel(entity, potionEffectType) + 1;

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)
                    && !entity.movement.isGliding()
                    && !entity.movement.isSwimming()
                    && !entity.movement.isCrawling()) {
                ItemStack boots = entity.getInventory().getBoots();
                Enchantment enchantment;

                if (boots != null && boots.containsEnchantment(enchantment = Enchantment.SOUL_SPEED)) {
                    SpartanLocation locationM1;

                    if (location.isBlock(Material.SOUL_SAND, BlockUtils.hitbox_max)
                            || location.isBlock(Material.SOUL_SOIL, BlockUtils.hitbox_max)
                            || (locationM1 = location.clone().add(0, -1, 0)).isBlock(Material.SOUL_SAND, BlockUtils.hitbox_max)
                            || locationM1.isBlock(Material.SOUL_SOIL, BlockUtils.hitbox_max)) {
                        entity.getCooldowns().add("move-utils=soul-speed-blocks", 20);
                    }

                    if (!entity.getCooldowns().canDo("move-utils=soul-speed-blocks")) {
                        level += boots.getEnchantmentLevel(enchantment);
                    }
                }
            }
        } else {
            level = getPotionLevel(entity, potionEffectType) + 1;
        }

        if (level > 0) {
            value += level * (value / divide);
        }
        return value;
    }

    public static float getWalkSpeedDifference(SpartanPlayer p) {
        String key = "player-data=walk-difference";
        float difference = Math.max(p.getWalkSpeed() - 0.2f, !p.movement.isFlying() ? 0.0f : (p.getFlySpeed() - 0.1f));

        if (difference > 0.0f) {
            p.getCooldowns().add(key, 2 * 20);
            p.getDecimals().set(key, difference);
        } else if (!p.getCooldowns().canDo(key)) {
            return (float) p.getDecimals().get(key, 1.0);
        }
        return Math.max(difference, 0.0f);
    }

    // Collisions

    public static int getNearbyCollisions(SpartanPlayer player, SpartanLocation location) {
        List<Entity> entities = player.getNearbyEntities(1.0, 1.0, 1.0);

        if (!entities.isEmpty()) {
            int count = 0;

            for (Entity entity : entities) {
                if (entity instanceof LivingEntity) {
                    if (CombatUtils.getWidthAndHeight(entity)[0] >= location.distance(entity.getLocation())) {
                        count++;
                    }
                } else if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                    BoundingBox boundingBox = entity.getBoundingBox();
                    double width = Math.max(
                            boundingBox.getMaxX() - boundingBox.getMinX(),
                            boundingBox.getMaxZ() - boundingBox.getMinZ()
                    );

                    if (width >= location.distance(entity.getLocation())) {
                        count++;
                    }
                } else {
                    count++;
                }
            }
            return count;
        } else {
            return 0;
        }
    }

    public static boolean hasNearbyCollisions(SpartanPlayer p) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                && (p.getTrackers().has(Trackers.TrackerType.ExtremeCollision)
                || getNearbyCollisions(p, p.movement.getLocation()) > 0);
    }

    // Potion Effects

    public static int getPotionEffectExtraTime(PotionEffectType potionEffectType) {
        return handledPotionEffects.getOrDefault(potionEffectType, 0);
    }

    public static int getPotionLevel(SpartanPlayer entity, PotionEffectType potionEffectType) {
        SpartanPotionEffect potionEffect = entity.getPotionEffect(
                potionEffectType,
                getPotionEffectExtraTime(potionEffectType)
        );

        if (potionEffect != null) {
            return potionEffect.bukkitEffect.getAmplifier();
        } else {
            return -1;
        }
    }

    // Ground

    public static boolean isOnGround(SpartanPlayer p, SpartanLocation loc, double x, double y, double z) {
        return x == 0.0 && y == 0.0 && z == 0.0 && loc.player != null
                ? loc.player.isOnGroundCustom()
                : GroundUtils.isOnGround(p, loc.clone().add(x, y, z), y, true, true);
    }

}
