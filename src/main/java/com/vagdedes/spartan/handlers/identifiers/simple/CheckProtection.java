package com.vagdedes.spartan.handlers.identifiers.simple;

import com.vagdedes.spartan.configuration.Compatibility;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckProtection {

    private static final Enums.HackType[] checksAffectedByCombatOrMovement;
    private static long cooldown = 0L, delay = 0L;

    static {
        Enums.HackType[] hackTypes = Enums.HackType.values();
        List<Enums.HackType> checksAffectedByCombatArray = new ArrayList<>(hackTypes.length);

        for (Enums.HackType hackType : hackTypes) {
            switch (hackType.getCheck().getCheckType()) {
                case MOVEMENT:
                case COMBAT:
                case EXPLOITS:
                    checksAffectedByCombatArray.add(hackType);
                    break;
                default:
                    break;
            }
        }
        checksAffectedByCombatOrMovement = checksAffectedByCombatArray.toArray(new Enums.HackType[0]);
    }

    public static void cancel(UUID uuid, int ticks, boolean ignoreNumbers) {
        SpartanBukkit.cooldowns.add(uuid + "=check=protection", ticks);

        if (ignoreNumbers) {
            SpartanBukkit.cooldowns.add(uuid + "=check=protection=ignore-numbers", ticks);
        }
    }

    public static void cancel(int ticks, int secondsDelay) {
        long ms = System.currentTimeMillis();

        if (delay <= ms) {
            if (secondsDelay > 0) {
                delay = ms + (secondsDelay * 1_000L);
            }
            cooldown = ms + (ticks * 50L);
        }
    }

    public static boolean hasCooldown(UUID uuid) {
        return cooldown >= System.currentTimeMillis()
                || !SpartanBukkit.cooldowns.canDo(uuid + "=check=protection");
    }

    public static void evadeCommonFalsePositives(SpartanPlayer player, Compatibility.CompatibilityType compatibilityType, Enums.HackType[] hackTypes, int ticks) {
        for (Enums.HackType hackType : hackTypes) {
            player.getViolations(hackType).addDisableCause(compatibilityType.toString(), null, ticks);
        }
    }

    public static void evadeStandardCombatFPs(SpartanPlayer player, Compatibility.CompatibilityType compatibilityType, int ticks) {
        evadeCommonFalsePositives(player, compatibilityType, checksAffectedByCombatOrMovement, ticks);
    }
}
