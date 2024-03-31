package com.vagdedes.spartan.functionality.identifiers.complex.unpredictable;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.damage.NoHitDelay;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class Damage { // todo replace

    private static final int blocksOffGround = 4;
    private static final String projectileSpamKey = "projectile-spam";

    // Handlers

    private static boolean selfHitHandling(SpartanPlayer p, Entity e) { // todo replicate and remove
        boolean result;

        if (e == null) {
            result = true;
        } else if (p.getVehicle() == null && !p.getHandlers().has(Handlers.HandlerType.ElytraUse)) {
            if (hasCooldown(p, null)
                    || Math.max(p.movement.getCustomDistance(), p.movement.getValueOrDefault(p.movement.getNmsDistance(), 0.0)) >= 0.18
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
                addCooldown(p, null, 40);
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
        }
        return false;
    }

    public static void runMove(SpartanPlayer p) {
        if (p.getHandlers().has(Handlers.HandlerType.Damage, projectileSpamKey)
                && p.getBlocksOffGround(3) <= 2) {
            p.getBuffer().remove(projectileSpamKey);
        }
    }

    // Return Methods

    public static boolean hasCooldown(SpartanPlayer p, String key) {
        boolean notNull = key != null;

        if (notNull
                ? p.getHandlers().has(Handlers.HandlerType.Damage, key)
                : p.getHandlers().has(Handlers.HandlerType.Damage)) {
            if (PlayerUtils.hasLevitationEffect(p)) {
                return true;
            } else {
            Double vertical = p.movement.getNmsVerticalDistance();

            if (vertical != null
                    && (p.movement.isFalling(vertical)
                    || p.movement.isJumping(vertical))) {
                return true;
            }
            int limit = NoHitDelay.hasCooldown(p) ? (blocksOffGround * 2) : blocksOffGround;
            return p.getBlocksOffGround(limit + 1) <= limit;
            }
        }
        return false;
    }

    // Modification Methods

    private static void addCooldown(SpartanPlayer p, String reason, int time) {
        Handlers handlers = p.getHandlers();
        handlers.add(Handlers.HandlerType.Damage, time);

        if (reason != null) {
            handlers.add(Handlers.HandlerType.Damage, reason, time);
        }
    }
}
