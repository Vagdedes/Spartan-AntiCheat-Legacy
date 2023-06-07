package me.vagdedes.spartan.utils.gameplay;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MoveUtils {

    public static final double[] falling = new double[]{0.07840, 0.07544, 0.09800},
            jumping = new double[]{0.08307, 0.42f, 0.0001}, // Last one is jump value precision
            climbing = new double[]{0.03684, 0.07531, 0.11215, 0.11760, 0.12529, 0.15, 0.15444}; // Attention to 'climbingScaffoldingMax'

    public static final double
            highPrecision = 0.0019,
            lowPrecision = highPrecision * 2.0,
            nearMaxFallingMotion = 3.87,
            chunk = 16.0,
            climbingScaffoldingMax = climbing[3] + 0.2;

    public static final int
            height,
            voidDamageHeight = -64,
            fallDamageBlocks = 4,
            chunkInt = AlgebraUtils.integerRound(chunk),
            jumpingMotions = 5;

    private static final Map<UUID, SpartanLocation> loc = new ConcurrentHashMap<>(Config.getMaxPlayers());
    private static final Set<Double> jumps = new HashSet<>(jumpingMotions);

    static {
        jumps.add(jumping[1]); // 0.41999
        jumps.add(0.33319);
        jumps.add(0.24813);
        jumps.add(0.16477);
        jumps.add(0.08307);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            List<World> worlds = Bukkit.getWorlds();

            if (worlds.size() > 0) {
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

        /*for (int i = 1; i <= 4; i++) {
            jumps.add(jumping[0] * i);
        }
        jumps.add(jumping[1]);*/
    }

    public static void run(SpartanPlayer p) {
        if (p.canRunChecks(false) || p.isFlying()) {
            SpartanLocation to = p.getLocation(),
                    from = getCachedLocation(p, true);

            if (from != null) {
                p.setCustomDistance(to.distance(from));
                p.setCustomHorizontalDistance(AlgebraUtils.getHorizontalDistance(to, from));
                p.setCustomVerticalDistance(to.getY() - from.getY());
            }
            loc.put(p.getUniqueId(), to); // Always last
        } else {
            remove(p);
        }
    }

    // Base

    public static void remove(SpartanPlayer p) {
        loc.remove(p.getUniqueId());
        p.setCustomDistance(0.0);
        p.setCustomHorizontalDistance(0.0);
        p.setCustomVerticalDistance(0.0);
        p.setNmsDistance(0.0);
        p.setNmsHorizontalDistance(0.0);
        p.setNmsVerticalDistance(0.0);
    }

    // Jumping

    public static boolean isJumping(double d, double diff) {
        if (d > 0.083 && d < 0.42 && !GroundUtils.heightExists(d, true)) {
            double original = jumping[1];

            for (double value : jumps) {
                if (Math.abs(value - d) < (value == original ? (diff / 2.0) : diff)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static double[] getClimbingMotionEdges() {
        double max = 0.0;
        double min = Double.MAX_VALUE;

        for (double value : climbing) {
            max = Math.max(max, value);
            min = Math.min(min, value);
        }
        return new double[]{min, max};
    }

    // Location

    public static SpartanLocation getCachedLocation(SpartanPlayer p, boolean returnNull) {
        SpartanLocation location = loc.get(p.getUniqueId());
        return returnNull ? location : (location == null ? p.getLocation() : location);
    }

    public static SpartanLocation getCachedLocation(SpartanPlayer p) {
        return getCachedLocation(p, false);
    }

    // Limits

    public static double calculateLimit(SpartanPlayer entity, SpartanLocation location, double value, double divide, PotionEffectType potionEffectType) {
        int level;

        if (potionEffectType == PotionEffectType.SPEED) {
            level = PlayerData.getPotionLevel(entity, potionEffectType);

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && !entity.isGliding() && !entity.isSwimming() && !entity.isCrawling()) {
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
                        if (level < 0) {
                            level = 0;
                        }
                        level += boots.getEnchantmentLevel(enchantment);
                    }
                }
            }
        } else {
            level = PlayerData.getPotionLevel(entity, potionEffectType);
        }

        if (level > -1) {
            value += (level + 1.0) * (value / divide);
        }
        return value;
    }

    // Scenario

    public static boolean isInWaterTunnel(SpartanPlayer p, SpartanLocation location) {
        return p.wasInLiquids() && PlayerData.hasDolphinsGraceEffect(p)
                && PlayerData.hasSoulSpeedEnchantment(p, location);
    }
}