package com.vagdedes.spartan.functionality.protections;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums;

public class Teleport {

    public static final int ticks = 3;
    public static final String reason = "Teleport-Protection";

    public static void run(SpartanPlayer p) {
        if (!p.getHandlers().isDisabled(Handlers.HandlerType.Teleport)) {
            if (Config.settings.getBoolean("Protections.use_teleport_protection")) {
                for (Enums.HackType hackType : Enums.HackType.values()) {
                    p.getViolations(hackType).addSilentCause(reason, null, ticks);
                }
            } else {
                for (Enums.HackType hackType : Enums.HackType.values()) {
                    Check check = hackType.getCheck();

                    if (!check.hasMaximumDefaultCancelViolation()) {
                        p.getViolations(hackType).addSilentCause(reason, null, ticks);
                    }
                }
            }
        }
    }
}
