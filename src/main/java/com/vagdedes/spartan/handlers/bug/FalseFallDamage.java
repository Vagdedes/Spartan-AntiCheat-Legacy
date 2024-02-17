package com.vagdedes.spartan.handlers.bug;

import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.handlers.stability.Moderation;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class FalseFallDamage {

    public static boolean runDamage(SpartanPlayer p, EntityDamageEvent.DamageCause dmg) {
        if (!Permissions.isBypassing(p, Enums.HackType.NoFall)) {
            LiveViolation violation = p.getViolations(Enums.HackType.NoFall);

            if (violation.getDisableCause() == null
                    && violation.getSilentCause() == null
                    && dmg == EntityDamageEvent.DamageCause.FALL

                    && !Moderation.wasDetected(p)
                    && p.getLastViolation().getLastViolationTime(false) > 500L
                    && p.hasViolations()

                    && !p.getHandlers().has(Handlers.HandlerType.Explosion)) {
                ItemStack itemStack = p.getItemInHand();

                if (itemStack.getType() != Material.WATER_BUCKET
                        && itemStack.getType() != Material.BUCKET
                        && (!p.isOnGround() || !p.isOnGroundCustom())) {
                    SpartanLocation to = p.getLocation();
                    return !PlayerData.isOnGround(p, to, 0, -1, 0)
                            && !PlayerData.isOnGround(p, to, 0, -1.5, 0);
                }
            }
        }
        return false;
    }
}
