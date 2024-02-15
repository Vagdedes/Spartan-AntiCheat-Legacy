package com.vagdedes.spartan.utils.java;

import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class HashHelper {

    public static final long
            trueHash = Boolean.hashCode(true),
            falseHash = Boolean.hashCode(false);
    private static final long
            bedWordHash = "BED".hashCode();

    public static long extendLong(long hash, long extra) {
        return (hash * SpartanBukkit.hashCodeMultiplierLong) + extra;
    }

    public static int extendInt(int hash, int extra) {
        return (hash * SpartanBukkit.hashCodeMultiplier) + extra;
    }

    // Separator

    public static <E> long hashDouble(double[] array, int generalize) {
        long result = 1L;
        boolean generalization = generalize > 0;

        for (double number : array) {
            result = extendLong(
                    result,
                    generalization
                            ? Double.hashCode(AlgebraUtils.cut(number, generalize))
                            : Double.hashCode(number)
            );
        }
        return result;
    }

    public static <E> long hashFloat(float[] array, int generalize) {
        long result = 1L;
        boolean generalization = generalize > 0;

        for (float number : array) {
            result = extendLong(
                    result,
                    generalization
                            ? Double.hashCode(AlgebraUtils.cut(number, generalize))
                            : Float.hashCode(number)
            );
        }
        return result;
    }

    // Separator

    public static <E> long fastCollection(Collection<E> collection) {
        Iterator<E> iterator = collection.iterator();
        long hash = (iterator.next().hashCode() * SpartanBukkit.hashCodeMultiplierLong) + collection.size();
        Object object = null;

        while (iterator.hasNext()) {
            object = iterator.next();
        }
        if (object != null) {
            hash = extendLong(hash, object.hashCode());
        }
        return hash;
    }

    public static <E> long collection(Collection<E> collection) {
        long result = 1L;

        for (Object object : collection) {
            result = extendLong(result, object.hashCode());
        }
        return result;
    }

    public static <E> long collection(Collection<E> collection, int start, int end) {
        long hash = 1L;
        int pos = 0;
        Iterator<E> iterator = collection.iterator();

        if (start > 0) {
            for (int i = 0; i < start; i++) {
                iterator.next();
            }
        }
        while (iterator.hasNext()) {
            if (pos >= end) {
                break;
            } else {
                hash = extendLong(hash, iterator.next().hashCode());
                pos++;
            }
        }
        return hash;
    }

    // Separator

    public static long hashPlayer(SpartanPlayer player) {
        long hash = Boolean.hashCode(player.isBedrockPlayer());
        Entity vehicle = player.getVehicle();
        Collection<PotionEffect> potionEffects;

        if (vehicle != null) {
            hash = extendLong(hash, vehicle.getType().toString().hashCode());

            if (vehicle instanceof LivingEntity) {
                potionEffects = ((LivingEntity) vehicle).getActivePotionEffects();
            } else {
                potionEffects = new ArrayList<>(0);
            }
        } else {
            EntityDamageEvent damage = player.getLastDamageCause();

            if (damage != null && TPS.getTick(player) == player.getDamageTick()) {
                hash = extendLong(hash, damage.getCause().ordinal());
            }
            hash = extendLong(hash, Double.hashCode(player.getEyeHeight()));
            hash = extendLong(hash, Boolean.hashCode(player.isGliding()));
            hash = extendLong(hash, Boolean.hashCode(player.isSwimming()));
            hash = extendLong(hash, Boolean.hashCode(player.isCrawling()));
            hash = extendLong(hash, Boolean.hashCode(player.isFrozen()));
            hash = extendLong(hash, Boolean.hashCode(player.isSneaking()));
            potionEffects = player.getActivePotionEffects();
        }
        if (!potionEffects.isEmpty()) {
            Map<Integer, PotionEffect> map = new TreeMap<>();

            for (PotionEffect potionEffect : potionEffects) {
                map.put(potionEffect.getType().toString().hashCode(), potionEffect);
            }
            for (PotionEffect potionEffect : map.values()) {
                hash = extendLong(hash, potionEffect.getType().toString().hashCode());
                hash = extendLong(hash, potionEffect.getAmplifier());
            }
        }
        return hash;
    }

    public static long hashInventory(SpartanPlayer player) {
        long hash = Boolean.hashCode(player.isUsingItem());
        ItemStack[] armors = player.getInventory().getArmorContents();

        for (ItemStack armor : armors) {
            if (armor != null) {
                hash = extendLong(hash, armor.getType().toString().hashCode());
                Map<Enchantment, Integer> enchantments = armor.getEnchantments();

                if (!enchantments.isEmpty()) {
                    Map<Integer, Integer> map = new TreeMap<>();

                    for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                        int key = entry.getKey().toString().hashCode();
                        map.put(
                                key,
                                (key * SpartanBukkit.hashCodeMultiplier) + entry.getValue()
                        );
                    }
                    for (int value : map.values()) {
                        hash = extendLong(hash, value);
                    }
                } else {
                    hash = extendLong(hash, falseHash);
                }
            } else {
                hash = extendLong(hash, falseHash);
            }
        }
        return hash;
    }

    public static long hashEnvironment(SpartanPlayer player,
                                       SpartanLocation location) {
        long hash = 1L;

        for (int y = -1; y <= Math.ceil(player.getEyeHeight()); y++) {
            for (SpartanLocation cloned : location.clone().getSurroundingLocations(1.0, y, 1.0, true)) {
                SpartanBlock block = cloned.getBlock();
                Material material = block.getType();

                if (BlockUtils.isSolid(material)) {
                    hash = extendLong(hash, GroundUtils.getHeightsHashLong(material, trueHash));
                    hash = extendLong(hash, Double.hashCode(AlgebraUtils.cut(location.distance(cloned), 1)));

                    if (BlockUtils.areBouncingBlocks(material)) {
                        hash = extendLong(
                                hash,
                                BlockUtils.areBeds(material)
                                        ? bedWordHash
                                        : material.toString().hashCode()
                        );
                    } else if (BlockUtils.areIceBlocks(material)) {
                        hash = extendLong(hash, material.toString().hashCode());
                    }
                } else if (block.isLiquid()) {
                    hash = extendLong(hash, material.toString().hashCode());
                } else {
                    hash = extendLong(hash, falseHash);
                }
                hash = extendLong(hash, Boolean.hashCode(block.isWaterLogged()));
            }
        }
        return hash;
    }
}
