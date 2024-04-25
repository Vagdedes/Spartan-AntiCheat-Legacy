package com.vagdedes.spartan.utils.gameplay;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.*;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class HashUtils {

    public static int extendInt(int hash, int extra) {
        return (hash * SpartanBukkit.hashCodeMultiplier) + extra;
    }

    // Separator

    public static <E> int fastAbstractCollection(Collection<E> collection) {
        int size = collection.size();
        return size > 0
                ? (size * SpartanBukkit.hashCodeMultiplier) + collection.iterator().next().hashCode()
                : 1;
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

    public static int hashPlayer(Entity vehicle,
                                 GameMode gameMode,
                                 ItemStack boots,
                                 Collection<SpartanPotionEffect> potionEffects,
                                 double eyeHeight,
                                 int ping,
                                 long lastFlight,
                                 boolean bedrockPlayer,
                                 boolean flying,
                                 boolean gliding,
                                 boolean swimming,
                                 boolean crawling,
                                 boolean frozen,
                                 boolean sneaking,
                                 boolean onGround) {
        int hash = Boolean.hashCode(bedrockPlayer);

        if (vehicle != null) {
            hash = extendInt(hash, vehicle.getType().toString().hashCode());
        } else {
            hash = extendInt(hash, Boolean.hashCode(lastFlight <= TPS.maximum));
            hash = extendInt(hash, Boolean.hashCode(flying));
            hash = extendInt(hash, gameMode.toString().hashCode());
            hash = extendInt(hash, Double.hashCode(eyeHeight));
            hash = extendInt(hash, Boolean.hashCode(gliding));
            hash = extendInt(hash, Boolean.hashCode(swimming));
            hash = extendInt(hash, Boolean.hashCode(crawling));
            hash = extendInt(hash, Boolean.hashCode(frozen));
            hash = extendInt(hash, Boolean.hashCode(sneaking));
            hash = extendInt(hash, Boolean.hashCode(onGround));
            hash = extendInt(hash, Math.min(AlgebraUtils.integerFloor(ping / TPS.tickTimeDecimal), 1_000));

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
            Map<Integer, SpartanPotionEffect> map = new TreeMap<>();

            for (SpartanPotionEffect potionEffect : potionEffects) {
                if (potionEffect.ticksPassed() <= TPS.maximum) {
                    map.put(potionEffect.bukkitEffect.getType().toString().hashCode(), potionEffect);
                }
            }
            for (SpartanPotionEffect potionEffect : map.values()) {
                hash = extendInt(hash, potionEffect.bukkitEffect.getType().toString().hashCode());
                hash = extendInt(hash, potionEffect.bukkitEffect.getAmplifier());
                hash = extendInt(hash, Long.hashCode(potionEffect.ticksPassed()));
            }
        }
        return hash;
    }

    public static int hashPlayer(SpartanPlayer player) {
        return hashPlayer(
                player.getVehicle(),
                player.getGameMode(),
                player.getInventory().getBoots(),
                player.getStoredPotionEffects(),
                player.getEyeHeight(),
                player.getPing(),
                player.movement.getLastFlight(),
                player.bedrockPlayer,
                player.movement.isFlying(),
                player.movement.isGliding(),
                player.movement.isSwimming(),
                player.movement.isCrawling(),
                player.isFrozen(),
                player.movement.isSneaking(),
                player.isOnGround()
        );
    }

    // Separator

    public static int hashEnvironment(int airTicks, double height, double previousHeight) {
        int hash = AlgebraUtils.floorToNearest(airTicks, (int) TPS.maximum);

        hash = extendInt(hash, Double.hashCode(AlgebraUtils.cut(
                height,
                GroundUtils.maxHeightLength
        )));
        hash = extendInt(hash, Double.hashCode(AlgebraUtils.cut(
                previousHeight,
                GroundUtils.maxHeightLength
        )));
        return hash;
    }

    public static int hashEnvironment(SpartanPlayer player,
                                      SpartanLocation location,
                                      SpartanLocation previousLocation) {
        return hashEnvironment(
                player.movement.getTicksOnAir(),
                location.getY() - location.getBlockY(),
                previousLocation.getY() - previousLocation.getBlockY()
        );
    }

    // Separator

    public static int hashTrackers(SpartanPlayer player) {
        int hash = 1;

        if (player.getVehicle() == null) {
            Set<Map.Entry<EntityDamageEvent.DamageCause, SpartanPlayerDamage>> damages = player.getRawReceivedDamages();

            if (!damages.isEmpty()) {
                Map<Integer, SpartanPlayerDamage> map = new TreeMap<>();

                for (Map.Entry<EntityDamageEvent.DamageCause, SpartanPlayerDamage> entry : damages) {
                    if (entry.getValue().ticksPassed() <= TPS.maximum) {
                        map.put(entry.getKey().toString().hashCode(), entry.getValue());
                    }
                }
                for (Map.Entry<Integer, SpartanPlayerDamage> entry : map.entrySet()) {
                    long ticksPassed = entry.getValue().ticksPassed();

                    if (ticksPassed <= TPS.maximum) {
                        EntityDamageByEntityEvent event = entry.getValue().getEntityDamageByEntityEvent();

                        if (event != null) {
                            hash = extendInt(hash, event.getDamager().getType().toString().hashCode());
                            hash = extendInt(hash, entry.getValue().getActiveItem().getType().toString().hashCode());
                        }
                        hash = extendInt(hash, entry.getKey());
                        hash = extendInt(hash, Long.hashCode(ticksPassed));
                        hash = extendInt(hash,
                                Double.hashCode(
                                        AlgebraUtils.cut(entry.getValue().location.distance(player.movement.getLocation()),
                                                1
                                        )
                                )
                        );
                    }
                }
            }
            List<SpartanPlayerVelocity> velocities = player.getVelocitiesReceived();

            if (!velocities.isEmpty()) {
                for (SpartanPlayerVelocity velocity : velocities) {
                    long ticksPassed = velocity.ticksPassed();

                    if (ticksPassed <= TPS.maximum) {
                        hash = extendInt(hash, Double.hashCode(velocity.event.getVelocity().getX()));
                        hash = extendInt(hash, Double.hashCode(velocity.event.getVelocity().getY()));
                        hash = extendInt(hash, Double.hashCode(velocity.event.getVelocity().getZ()));
                        hash = extendInt(hash, Long.hashCode(ticksPassed));
                        hash = extendInt(hash,
                                Double.hashCode(
                                        AlgebraUtils.cut(velocity.location.distance(player.movement.getLocation()),
                                                1
                                        )
                                )
                        );
                    }
                }
            }
        }
        hash = HashUtils.extendInt(
                hash,
                player.getTrackers().getRemainingTicks(Trackers.TrackerType.Piston)
        );
        hash = HashUtils.extendInt(
                hash,
                player.getTrackers().getRemainingTicks(Trackers.TrackerType.Trident)
        );
        hash = HashUtils.extendInt(
                hash,
                player.getTrackers().getRemainingTicks(Trackers.TrackerType.BubbleWater, "soul-sand")
        );
        hash = HashUtils.extendInt(
                hash,
                player.getTrackers().getRemainingTicks(Trackers.TrackerType.BubbleWater, "magma-block")
        );
        hash = HashUtils.extendInt(
                hash,
                player.getTrackers().getRemainingTicks(Trackers.TrackerType.BouncingBlocks, "slime")
        );
        hash = HashUtils.extendInt(
                hash,
                player.getTrackers().getRemainingTicks(Trackers.TrackerType.BouncingBlocks, "bed")
        );
        long lastLiquid = player.movement.getLastLiquid();

        if (lastLiquid <= TPS.maximum) {
            hash = extendInt(hash, Long.hashCode(lastLiquid));
        }
        return hash;
    }
}
