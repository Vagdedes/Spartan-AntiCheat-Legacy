package com.vagdedes.spartan.utils.java;

import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class HashHelper {

    public static final int
            trueHash = Boolean.hashCode(true),
            falseHash = Boolean.hashCode(false);
    private static final int
            bedWordHash = "BED".hashCode();

    public static int extend(int hash, int extra) {
        return (hash * SpartanBukkit.hashCodeMultiplier) + extra;
    }

    // Separator

    public static <E> int hashDouble(double[] array, int generalize) {
        int result = 1;
        boolean generalization = generalize > 0;

        for (double number : array) {
            result = extend(
                    result,
                    generalization
                            ? Double.hashCode(AlgebraUtils.cut(number, generalize))
                            : Double.hashCode(number)
            );
        }
        return result;
    }

    public static <E> int hashFloat(float[] array, int generalize) {
        int result = 1;
        boolean generalization = generalize > 0;

        for (float number : array) {
            result = extend(
                    result,
                    generalization
                            ? Double.hashCode(AlgebraUtils.cut(number, generalize))
                            : Float.hashCode(number)
            );
        }
        return result;
    }

    // Separator

    public static <E> int fastCollection(Collection<E> collection) {
        Iterator<E> iterator = collection.iterator();
        int hash = (iterator.next().hashCode() * SpartanBukkit.hashCodeMultiplier) + collection.size();
        Object object = null;

        while (iterator.hasNext()) {
            object = iterator.next();
        }
        if (object != null) {
            hash = extend(hash, object.hashCode());
        }
        return hash;
    }

    public static <E> int collection(Collection<E> collection) {
        int result = 1;

        for (Object object : collection) {
            result = extend(result, object.hashCode());
        }
        return result;
    }

    public static <E> int collection(Collection<E> collection, int start, int end) {
        int result = 1, pos = 0;
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
                result = extend(result, iterator.next().hashCode());
                pos++;
            }
        }
        return result;
    }

    // Separator

    public static int hashPlayer(SpartanPlayer player) {
        int hash = Boolean.hashCode(player.isBedrockPlayer());
        Entity vehicle = player.getVehicle();
        Collection<PotionEffect> potionEffects;

        if (vehicle != null) {
            hash = extend(hash, vehicle.getType().toString().hashCode());

            if (vehicle instanceof LivingEntity) {
                potionEffects = ((LivingEntity) vehicle).getActivePotionEffects();
            } else {
                potionEffects = new ArrayList<>(0);
            }
        } else {
            EntityDamageEvent damage = player.getLastDamageCause();

            if (damage != null && TPS.getTick(player) == player.getDamageTick()) {
                hash = extend(hash, damage.getCause().ordinal());
            }
            hash = extend(hash, Double.hashCode(player.getEyeHeight()));
            hash = extend(hash, Boolean.hashCode(player.isGliding()));
            hash = extend(hash, Boolean.hashCode(player.isSwimming()));
            hash = extend(hash, Boolean.hashCode(player.isCrawling()));
            hash = extend(hash, Boolean.hashCode(player.isFrozen()));
            potionEffects = player.getActivePotionEffects();
        }
        if (!potionEffects.isEmpty()) {
            Map<Integer, PotionEffect> map = new TreeMap<>();

            for (PotionEffect potionEffect : potionEffects) {
                map.put(potionEffect.getType().toString().hashCode(), potionEffect);
            }
            for (PotionEffect potionEffect : map.values()) {
                hash = extend(hash, potionEffect.getType().toString().hashCode());
                hash = extend(hash, potionEffect.getAmplifier());
            }
        }
        return hash;
    }

    public static int hashInventory(SpartanPlayer player) {
        int hash = Boolean.hashCode(player.isUsingItem());
        ItemStack[] armors = player.getInventory().getArmorContents();

        for (ItemStack armor : armors) {
            if (armor != null) {
                hash = extend(hash, armor.getType().toString().hashCode());
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
                        hash = extend(hash, value);
                    }
                } else {
                    hash = extend(hash, falseHash);
                }
            } else {
                hash = extend(hash, falseHash);
            }
        }
        return hash;
    }

    public static int hashEnvironment(SpartanLocation location) {
        int hash = 1;

        for (int y = -1; y <= 2; y++) {
            for (SpartanLocation cloned : location.getSurroundingLocations(1.0, y, 1.0, true)) {
                SpartanBlock block = cloned.getBlock();
                Material material = block.getType();

                if (BlockUtils.isSolid(material)) {
                    hash = extend(hash, GroundUtils.getHeightsHash(material, trueHash));
                    hash = extend(hash, Double.hashCode(AlgebraUtils.cut(location.distance(cloned), 1)));

                    if (BlockUtils.areBouncingBlocks(material)) {
                        hash = extend(
                                hash,
                                BlockUtils.areBeds(material)
                                        ? bedWordHash
                                        : material.toString().hashCode()
                        );
                    }
                } else if (block.isLiquid()) {
                    hash = extend(hash, material.toString().hashCode());
                } else {
                    hash = extend(hash, falseHash);
                }
                hash = extend(hash, Boolean.hashCode(block.isWaterLogged()));
            }
        }
        return hash;
    }
}
