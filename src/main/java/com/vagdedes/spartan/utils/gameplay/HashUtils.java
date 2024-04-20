package com.vagdedes.spartan.utils.gameplay;

import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayerDamage;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class HashUtils {

    public static final int loopHash = Boolean.hashCode(false);

    public static int extendInt(int hash, int extra) {
        return (hash * SpartanBukkit.hashCodeMultiplier) + extra;
    }

    // Separator

    public static <E> int fastAbstractCollection(Collection<E> collection) {
        int size = collection.size();
        return size > 0 ? (size * SpartanBukkit.hashCodeMultiplier) + collection.iterator().next().hashCode() : 1;
    }

    public static int collection(Collection<Float> collection) {
        int hash = 1;

        for (float value : collection) {
            hash = extendInt(hash, Float.hashCode(value));
        }
        return hash;
    }

    public static int collection(Collection<Number> collection, int fromIndex, int toIndex) {
        Iterator<Number> iterator = collection.iterator();

        if (fromIndex > 0) {
            for (int i = 0; i < fromIndex; i++) {
                if (iterator.hasNext()) {
                    iterator.next();
                } else {
                    return 1;
                }
            }

        }
        int hash = 1;
        int pos = fromIndex;

        while (iterator.hasNext()) {
            hash = extendInt(hash, iterator.next().hashCode());

            if (pos == toIndex) {
                return hash;
            }
            pos++;
        }
        return 1;
    }

    // Separator

    public static int hashPlayer(SpartanPlayer player) {
        int hash = Boolean.hashCode(player.bedrockPlayer);
        Entity vehicle = player.getVehicle();
        Collection<PotionEffect> potionEffects;

        if (vehicle != null) {
            hash = extendInt(hash, vehicle.getType().toString().hashCode());

            if (vehicle instanceof LivingEntity) {
                potionEffects = ((LivingEntity) vehicle).getActivePotionEffects();
            } else {
                potionEffects = new ArrayList<>(0);
            }
        } else {
            Set<Map.Entry<EntityDamageEvent.DamageCause, SpartanPlayerDamage>> damages = player.getRawReceivedDamages();

            if (!damages.isEmpty()) {
                for (Map.Entry<EntityDamageEvent.DamageCause, SpartanPlayerDamage> entry : damages) {
                    long ticksPassed = entry.getValue().ticksPassed();

                    if (ticksPassed <= TPS.maximum) {
                        EntityDamageByEntityEvent event = entry.getValue().getEntityDamageByEntityEvent();

                        if (event != null) {
                            hash = extendInt(hash, event.getDamager().getType().toString().hashCode());
                            hash = extendInt(hash, entry.getValue().getActiveItem().getType().toString().hashCode());
                        }
                        hash = extendInt(hash, entry.getKey().toString().hashCode());
                        hash = extendInt(hash, Long.hashCode(ticksPassed));
                        hash = extendInt(hash,
                                Double.hashCode(
                                        AlgebraUtils.cut(entry.getValue().location.distance(player.movement.getLocation()),
                                                1
                                        )
                                )
                        );
                    } else {
                        hash = extendInt(hash, loopHash);
                    }
                }
            }
            hash = extendInt(hash, player.getGameMode().toString().hashCode());
            hash = extendInt(hash, Double.hashCode(player.getEyeHeight()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isGliding()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isSwimming()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isCrawling()));
            hash = extendInt(hash, Boolean.hashCode(player.isFrozen()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isSneaking()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isFlying()));
            hash = extendInt(hash, Boolean.hashCode(player.isOnGround()));
            hash = extendInt(hash, Math.min(AlgebraUtils.integerFloor(player.getPing() / TPS.tickTimeDecimal), 1000));
            potionEffects = player.getActivePotionEffects();
            ItemStack boots = player.getInventory().getBoots();

            if (boots != null) {
                Map<Enchantment, Integer> enchantments = boots.getEnchantments();

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
                        hash = extendInt(hash, value);
                    }
                }
            }
        }
        if (!potionEffects.isEmpty()) {
            Map<Integer, PotionEffect> map = new TreeMap<>();

            for (PotionEffect potionEffect : potionEffects) {
                map.put(potionEffect.getType().toString().hashCode(), potionEffect);
            }
            for (PotionEffect potionEffect : map.values()) {
                hash = extendInt(hash, potionEffect.getType().toString().hashCode());
                hash = extendInt(hash, potionEffect.getAmplifier());
            }
        }
        return hash;
    }

    public static int hashEnvironment(SpartanPlayer player,
                                      SpartanLocation location,
                                      SpartanLocation previousLocation) {
        int hash = AlgebraUtils.floorToNearest(player.movement.getTicksOnAir(), (int) TPS.maximum);

        if (player.movement.getValueOrDefault(player.movement.getNmsVerticalDistance(), 0.0) >= 0.0) {
            hash = extendInt(hash, Double.hashCode(
                    AlgebraUtils.cut(location.getY() - location.getBlockY(), GroundUtils.maxHeightLength))
            );
            hash = extendInt(hash, Double.hashCode(
                    AlgebraUtils.cut(previousLocation.getY() - previousLocation.getBlockY(), GroundUtils.maxHeightLength)
            ));
        }
        return hash;
    }
}
