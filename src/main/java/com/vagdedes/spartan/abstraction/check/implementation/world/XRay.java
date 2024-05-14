package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class XRay extends CheckExecutor {

    static {
        if (Config.settings.getBoolean("Important.modify_server_configuration")) {
            String key = "Config.settings.world-Config.settings.default.anti-xray";
            File file = new File(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19) ? "/config/paper-global.yml" : "paper.yml");

            if (file.exists() && file.isFile()) {
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

                if (configuration.contains(key)) {
                    ConfigUtils.set(file, key + ".enabled", true);
                    ConfigUtils.set(file, key + ".engine-mode", 2);
                } else {
                    key = "world-Config.settings.default.anti-xray";

                    if (configuration.contains(key)) {
                        ConfigUtils.set(file, key + ".enabled", true);
                        ConfigUtils.set(file, key + ".engine-mode", 2);
                    }
                }
            }
        }
    }

    public XRay(SpartanPlayer player) {
        super(Enums.HackType.XRay, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        Object[] objects = (Object[]) object;
        MiningHistory miningHistory = (MiningHistory) objects[1];
        int mines = miningHistory.getMines(World.Environment.valueOf(objects[0].toString()));

        if (mines > 0) {
            double globalAverage = ResearchEngine.getMiningHistoryAverage((Enums.MiningOre) objects[2]),
                    average = mines / ((double) miningHistory.getDays());

            if (globalAverage < 0.0) {
                globalAverage = Math.abs(globalAverage);
            } else {
                globalAverage = globalAverage * 1.5;
            }
            double points = AlgebraUtils.cut(average - globalAverage, 2);

            if (points > 0.0) {
                cancel(
                        "Unfair gathering of " + objects[3] + ", surpassed others by " + points + " " + (points == 1.0 ? "point" : "points"));
            }
        }
    }

    @Override
    protected boolean canRun() {
        return player.getWorld().getWorldType() != WorldType.FLAT
                && !player.getViolations(hackType).hasLevel();
    }

}
