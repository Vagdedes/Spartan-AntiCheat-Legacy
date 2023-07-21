package me.vagdedes.spartan.checks.world;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.notifications.AwarenessNotifications;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.MiningHistory;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.ConfigUtils;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class XRay {

    public static final Enums.HackType check = Enums.HackType.XRay;
    public static final double
            lenientStatisticalRatio = 0.8,
            strictStatisticalRatio = 2.0 - lenientStatisticalRatio;

    static {
        if (check.getCheck().isEnabled(null, null)
                && Settings.getBoolean("Important.modify_server_configuration")) {
            String key = "settings.world-settings.default.anti-xray";
            File file = new File(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) ? "/config/paper-global.yml" : "paper.yml");

            if (file.exists() && file.isFile()) {
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

                if (configuration.contains(key)) {
                    ConfigUtils.set(file, key + ".enabled", true);
                    ConfigUtils.set(file, key + ".engine-mode", 2);
                } else {
                    key = "world-settings.default.anti-xray";

                    if (configuration.contains(key)) {
                        ConfigUtils.set(file, key + ".enabled", true);
                        ConfigUtils.set(file, key + ".engine-mode", 2);
                    }
                }
            }
        }
    }

    public static void run(SpartanPlayer p, World.Environment environment,
                           MiningHistory miningHistory, Enums.MiningOre ore, String pluralKey) {
        if (!p.canRunCheck(check, false)
                || !p.canDo(true)
                || p.getWorld().getWorldType() == WorldType.FLAT
                || check.getCheck().getViolations(p).hasLevel()) {
            return;
        }
        int mines = miningHistory.getMines(environment);

        if (mines > 0) {
            double globalAverage = ResearchEngine.getMiningHistoryAverage(ore, strictStatisticalRatio);

            if (globalAverage < 0.0) { // When below zero, it means data is not yet coming from player profiles but backup variables
                globalAverage = Math.abs(globalAverage);
                String checkDefaultName = check.toString(),
                        checkName = check.getCheck().getName(),
                        message = AwarenessNotifications.getOptionalNotification(
                                "The '" + (checkName.equals(checkDefaultName) ? checkDefaultName : checkDefaultName + " (" + checkName + ")") + "' check has insufficient data to work with. "
                                        + "If you plan on testing this check, try again in a few days and after normal everyday players have had the chance to mine blocks."
                        );

                if (message != null) {
                    List<SpartanPlayer> players = Permissions.getStaff();

                    if (players.size() > 0) {
                        for (SpartanPlayer o : players) {
                            if (AwarenessNotifications.canSend(o.getUniqueId(), check.toString())) {
                                o.sendMessage(message);
                            }
                        }
                    }
                }
            }
            double average = mines / ((double) miningHistory.getDays());

            if (average > globalAverage) {
                double points = AlgebraUtils.cut(average - globalAverage, 2);
                new HackPrevention(p, check,
                        "Unfair gathering of " + pluralKey + ", surpassed others by " + points + " " + (points == 1.0 ? "point" : "points"));
            }
        }
    }
}
