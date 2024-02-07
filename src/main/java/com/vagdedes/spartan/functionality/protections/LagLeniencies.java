package com.vagdedes.spartan.functionality.protections;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.configuration.Settings;
import com.vagdedes.spartan.handlers.stability.Cache;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.LagLeniency;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class LagLeniencies {

    private static final int
            cooldownTicks = 20,
            criticalMinimumPingDifference = 33,
            criticalMaximumPingDifference = criticalMinimumPingDifference * 3,
            criticalPingRequired = TPS.tickTimeInteger + criticalMinimumPingDifference,
            maxConsideredPing = 1000;

    private static final Map<UUID, LagLeniency> data = Cache.store(new LinkedHashMap<>());

    public static void remove(SpartanPlayer p) {
        data.remove(p.getUniqueId());
    }

    // Separator

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
                                maxConsideredPing
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

    // Spearator

    public static boolean hasInconsistencies(SpartanPlayer p, String key) {
        // Null = Calculate, Empty Key = Basics, Normal key = Cache
        String structure = "lag-leniences=feature=";

        if (key == null) {
            boolean tps = Config.settings.getBoolean(Settings.tpsProtectionOption),
                    ping = Config.settings.getInteger(Settings.maxSupportedLatencyOption) > 0;

            if (tps || ping) {
                boolean result = false;
                UUID uuid = p.getUniqueId();
                LagLeniency data = LagLeniencies.data.get(uuid);

                if (data != null) {
                    // TPS Protection
                    if (tps) {
                        double current = TPS.get(p, false);

                        if (current < TPS.excellent) {
                            double previous = data.getTPS();

                            if (previous > 0.0 && (previous - TPS.get(p, false)) >= TPS.criticalDifference) {
                                TPS.setProtection(p, cooldownTicks);
                                result = true;
                            }
                        }
                    }

                    // Latency Protection
                    if (ping && data.getCreationTime() <= 30_000L) {
                        int current = p.getPing();

                        if (current >= criticalPingRequired) {
                            int previous = data.getPing();

                            if (previous > 0 // Ensure the previous latency has been calculated
                                    && current >= (previous + criticalMinimumPingDifference) // Check if the threshold has been exceeded
                                    && (current - previous) <= criticalMaximumPingDifference) { // Limit the threshold to account for ping-spoof
                                p.getCooldowns().add(structure + "ping", cooldownTicks);
                                result = true;
                            }
                        }
                    }
                }
                LagLeniencies.data.put(uuid, new LagLeniency(p));
                return result || hasInconsistencies(p, "");
            }
            return false;
        }
        return key.isEmpty() ? (TPS.areDropping(p) || !p.getCooldowns().canDo(structure + "ping")) :
                (key.equals("tps") ? TPS.areDropping(p) : !p.getCooldowns().canDo(structure + key));
    }
}
