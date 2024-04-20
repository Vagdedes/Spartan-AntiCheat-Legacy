package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayerDamage;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.CombatUtils;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CombatProcessing {

    private static final int pastTicks = 5;
    private static final String
            baseKey = "combat=",
            cooldownKey = "=cooldown";
    private static final Map<UUID, Vector[]> directionalComparison = Cache.store(new LinkedHashMap<>(Config.getMaxPlayers()));
    public static final String
            yaw = "yaw",
            pitch = "pitch",
            yawDifference = yaw + "-difference",
            pitchDifference = pitch + "-difference";

    public static void runMove(SpartanPlayer p, SpartanLocation to) {
        Vector[] directions = directionalComparison.get(p.uuid);
        float yawVar = to.getYaw(), pitchVar = to.getPitch();

        if (directions != null) {
            Vector yawDirection = CombatUtils.getDirection(yawVar, 0.0f);
            p.getDecimals().add(baseKey + yawDifference, Math.toDegrees(yawDirection.distance(directions[0])), pastTicks);
            p.getCooldowns().add(baseKey + yawDifference + cooldownKey, pastTicks);
            Vector pitchDirection = CombatUtils.getDirection(0.0f, pitchVar);
            p.getDecimals().add(baseKey + pitchDifference, Math.toDegrees(pitchDirection.distance(directions[1])), pastTicks);
            p.getCooldowns().add(baseKey + pitchDifference + cooldownKey, pastTicks);
            directionalComparison.put(p.uuid, new Vector[]{yawDirection, pitchDirection});
        } else {
            directionalComparison.put(p.uuid, new Vector[]{
                    CombatUtils.getDirection(yawVar, 0.0f),
                    CombatUtils.getDirection(0.0f, pitchVar)
            });
        }
        p.getDecimals().add(baseKey + yaw, yawVar, pastTicks);
        p.getCooldowns().add(baseKey + yaw + cooldownKey, pastTicks);
        p.getDecimals().add(baseKey + pitch, pitchVar, pastTicks);
        p.getCooldowns().add(baseKey + pitch + cooldownKey, pastTicks);
    }

    public static double getDecimal(SpartanPlayer p, String s, double fallback) {
        return !p.getCooldowns().canDo(baseKey + s + cooldownKey) ?
                p.getDecimals().get(baseKey + s, fallback, Decimals.CALCULATE_MAX) :
                fallback;
    }

    public static int getInteger(SpartanPlayer p, String s, int fallback) {
        return !p.getCooldowns().canDo(baseKey + s + cooldownKey) ?
                (int) p.getDecimals().get(baseKey + s, fallback, Decimals.CALCULATE_MAX) :
                fallback;
    }

    public static List<Double> getAllValues(SpartanPlayer p, String s) {
        return p.getDecimals().getOldestToNewestList(baseKey + s);
    }

    // Separator: Use for damage causes that still have an effect even after not being the last one

    public static boolean hasExplosionDamage(SpartanPlayer player) {
        for (EntityDamageEvent.DamageCause cause : new EntityDamageEvent.DamageCause[]{
                EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
                EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
        }) {
            if (player.getDamageReceived(cause).ticksPassed() <= TPS.tickTime) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBigKnockBackDamage(SpartanPlayer player) {
        for (SpartanPlayerDamage damage : player.getReceivedDamages()) {
            if (damage.hasBigKnockBack()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSignificantDamage(SpartanPlayer player) {
        for (SpartanPlayerDamage damage : player.getReceivedDamages()) {
            if (damage.isSignificant()) {
                return true;
            }
        }
        return false;
    }
}
