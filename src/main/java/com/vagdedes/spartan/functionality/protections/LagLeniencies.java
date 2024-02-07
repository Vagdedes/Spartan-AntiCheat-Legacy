package com.vagdedes.spartan.functionality.protections;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.configuration.Settings;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;

public class LagLeniencies {

    public static double getDelay(SpartanPlayer p) {
        double pingDelay;

        if (Config.settings.getInteger(Settings.maxSupportedLatencyOption) <= 0
                || p.getProfile().isSuspectedOrHacker()) {
            pingDelay = 0.0;
        } else {
            int latency = p.getPing();

            if (latency <= TPS.tickTimeInteger) {
                pingDelay = 0.0;
            } else {
                pingDelay = Math.min(
                        latency,
                        Math.min(
                                Config.settings.getInteger("Protections.max_supported_player_latency"),
                                1000
                        )
                );
            }
        }

        // Separator
        double tpsDelay;

        if (!Config.settings.getBoolean(Settings.tpsProtectionOption)) {
            tpsDelay = 0.0;
        } else {
            double tps = TPS.get(p, false);

            if (tps >= TPS.excellent) {
                tpsDelay = 0.0;
            } else {
                tpsDelay = TPS.maximum - tps;
            }
        }

        // Separator
        return Math.min(
                Math.max(
                        tpsDelay, pingDelay > TPS.tickTimeInteger
                                ? (pingDelay - TPS.tickTimeInteger) / TPS.tickTimeDecimal
                                : 0
                ),
                TPS.maximum
        );
    }

    public static double getDelaySimplified(double delay) {
        return delay > 0.0 && delay < 0.5 ? 0.5 : AlgebraUtils.roundToHalf(delay);
    }
}
