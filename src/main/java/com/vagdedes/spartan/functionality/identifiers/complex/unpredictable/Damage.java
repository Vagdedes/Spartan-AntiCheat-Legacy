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

    // Runnables

    public static void runReceiveDamage(DamageCause cause, SpartanPlayer p, Entity damager) {
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && cause == EntityDamageEvent.DamageCause.DRAGON_BREATH) {
            if (damager instanceof EnderDragon
                    || damager instanceof Fireball
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) && damager instanceof Warden) {
                int time = !p.isOnGround() || !p.isOnGroundCustom() ? 120 : 60;
                p.getHandlers().add(Handlers.HandlerType.Velocity, time);
                addCooldown(p, time);
            } else if (damager instanceof Golem) {
                addCooldown(p, 80);
            } else {
                addCooldown(p, 40);
            }
        } else if (MythicMobs.is(damager) || ItemsAdder.is(damager)) {
            addCooldown(p, 40);
        }
    }

    // Modification Methods

    private static void addCooldown(SpartanPlayer p, int time) {
        Handlers handlers = p.getHandlers();
        handlers.add(Handlers.HandlerType.Damage, time);
    }
}
