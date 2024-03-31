package com.vagdedes.spartan.functionality.identifiers.complex.unpredictable;

import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

public class Damage { // todo replace

    public static final String
            selfHitKey = "self-hit",
            knockbackKey = "knockback",
            fishingHookKey = "fishing-hook";
    private static final int blocksOffGround = 4;
    private static final String projectileSpamKey = "projectile-spam";

    // Handlers

    private static boolean selfHitHandling(SpartanPlayer p, Entity e) {
        boolean result;

        if (e == null) {
            result = true;
        } else if (p.getVehicle() == null && !p.getHandlers().has(Handlers.HandlerType.ElytraUse)) {
            if (hasCooldown(p, null)
                    || Math.max(p.movement.getCustomDistance(), p.movement.getValueOrDefault(p.movement.getNmsDistance(), 0.0)) >= 0.18
                    || !p.getHandlers().has(Handlers.HandlerType.Damage, selfHitKey + "=" + e.getUniqueId())
                    || p.getItemInHand().containsEnchantment(Enchantment.ARROW_KNOCKBACK)) {
                result = true;
            } else {
                SpartanLocation to = p.movement.getLocation();
                result = p.movement.wasInLiquids()
                        || !p.isOnGroundCustom() && !PlayerUtils.isOnGround(p, to, 0, -1, 0);
            }
        } else {
            result = false;
        }

        if (result) {
            addCooldown(p, selfHitKey, 25);
        }
        return result;
    }

    // Runnables

    public static void runReceiveDamage(DamageCause cause, SpartanPlayer p, Entity damager) {
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && cause == EntityDamageEvent.DamageCause.DRAGON_BREATH) {
            if (damager instanceof EnderDragon
                    || damager instanceof Fireball
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && damager instanceof Warden) {
                int time = !p.isOnGround() || !p.isOnGroundCustom() ? 120 : 60;
                p.getHandlers().add(Handlers.HandlerType.Velocity, time);
                addCooldown(p, null, time);
            } else if (damager instanceof Golem) {
                addCooldown(p, null, 80);
            } else {
                SpartanPlayer t = SpartanBukkit.getPlayer((Player) damager);

                if (t == null) {
                    addCooldown(p, null, 40);
                } else {
                    int level = t.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK);

                    if (level > 2) {
                        p.getHandlers().add(Handlers.HandlerType.Velocity, 100);
                    } else if (level > 0) {
                        addCooldown(p, knockbackKey, 60);
                    } else {
                        addCooldown(p, null, 40);
                    }
                }
            }
        } else if (MythicMobs.is(damager) || ItemsAdder.is(damager)) {
            addCooldown(p, null, 40);
        }
    }

    public static boolean runDealAndReceiveDamage(Entity damager, Entity entity, DamageCause dmg, boolean cancelled) {
        if (!cancelled
                && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)
                && damager instanceof Player
                && dmg == DamageCause.ENTITY_ATTACK
                && entity instanceof Guardian) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) damager);

            if (p != null) {
                addCooldown(p, null, 40);
            }
        } else if (damager instanceof Projectile && entity instanceof Player && dmg == DamageCause.PROJECTILE) {
            SpartanPlayer p = SpartanBukkit.getPlayer((Player) entity);

            if (p == null) {
                return false;
            }
            Projectile projectile = (Projectile) damager;

            if (projectile.getShooter() instanceof Player) {
                Player nt = ((Player) projectile.getShooter());
                SpartanPlayer t = SpartanBukkit.getPlayer(nt);

                if (t == null) {
                    return false;
                }
                ItemStack item = t.getItemInHand();
                int enchantment = Math.max(item.getEnchantmentLevel(Enchantment.KNOCKBACK), item.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK));

                if (enchantment > 0) {
                    boolean can;

                    if (t.equals(p)) {
                        Buffer buffer = p.getBuffer();
                        Handlers handlers = p.getHandlers();
                        SpartanLocation to = p.movement.getLocation();

                        if (p.isOnGroundCustom() || PlayerUtils.isOnGround(p, to, 0, -1, 0) || PlayerUtils.isOnGround(p, to, 0, -2, 0)) {
                            buffer.remove(projectileSpamKey);
                            can = true;
                        } else {
                            can = buffer.increase(projectileSpamKey, 1) < 5 // threshold hasn't been met
                                    || !handlers.has(Handlers.HandlerType.Damage, projectileSpamKey); // time has passed
                        }
                        handlers.add(Handlers.HandlerType.Damage, projectileSpamKey, 40);
                    } else {
                        can = true;
                    }

                    if (can) {
                        int ticks = 80;
                        addCooldown(p, knockbackKey, ticks);

                        if (enchantment > 2) {
                            p.getHandlers().add(Handlers.HandlerType.Velocity, ticks);
                        }
                    }
                } else if (!t.equals(p)) {
                    addCooldown(p, null, 40);
                }

                // Do not connect with else statement
                if (!selfHitHandling(p, damager)) {
                    return Config.settings.getBoolean("Protections.avoid_self_bow_damage") && !Permissions.isBypassing(p, null);
                }
            } else if (projectile.getShooter() != null) {
                if (projectile.getShooter() instanceof EnderDragon) {
                    int ticks = 60;
                    p.getHandlers().add(Handlers.HandlerType.Velocity, ticks);
                    addCooldown(p, null, ticks);
                } else {
                    addCooldown(p, null, 30);
                }
            } else {
                p.movement.setAirTicks(0);
                selfHitHandling(p, null);
            }
        }
        return false;
    }

    public static void runMove(SpartanPlayer p) {
        if (p.getHandlers().has(Handlers.HandlerType.Damage, projectileSpamKey)
                && p.getBlocksOffGround(3) <= 2) {
            p.getBuffer().remove(projectileSpamKey);
        }
    }

    public static void runBow(SpartanPlayer p, Entity projectile) {
        if (p.movement.getLocation().getPitch() <= -60.0f) {
            p.getHandlers().add(Handlers.HandlerType.Damage, selfHitKey + "=" + projectile.getUniqueId(), 200);
        }
    }

    // Return Methods

    public static boolean hasCooldown(SpartanPlayer p, String key) {
        boolean notNull = key != null;

        if (notNull
                ? p.getHandlers().has(Handlers.HandlerType.Damage, key)
                : p.getHandlers().has(Handlers.HandlerType.Damage)) {
            if (notNull && key.equals(selfHitKey)
                    || PlayerUtils.hasLevitationEffect(p)) {
                return true;
            }
            Double vertical = p.movement.getNmsVerticalDistance();

            if (vertical != null
                    && (p.movement.isFalling(vertical)
                    || p.movement.isJumping(vertical))) {
                return true;
            }
            int limit = NoHitDelay.hasCooldown(p) ? (blocksOffGround * 2) : blocksOffGround;
            return p.getBlocksOffGround(limit + 1) <= limit;
        }
        return false;
    }

    // Modification Methods

    public static void addCooldown(SpartanPlayer p, String reason, int time) {
        Handlers handlers = p.getHandlers();
        handlers.add(Handlers.HandlerType.Damage, time);

        if (reason != null) {
            handlers.add(Handlers.HandlerType.Damage, reason, time);
        }
    }
}
