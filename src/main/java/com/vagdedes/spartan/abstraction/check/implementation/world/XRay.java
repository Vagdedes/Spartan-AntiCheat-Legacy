package com.vagdedes.spartan.abstraction.check.implementation.world;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class XRay extends CheckExecutor {

    private static double getAverageDeviation(World.Environment environment, MiningHistory.MiningOre ore) {
        Collection<PlayerProfile> profiles = ResearchEngine.getPlayerProfiles();

        if (profiles.size() <= TPS.maximum) {
            return 0.0;
        } else {
            int profileCount = 0;
            Map<String, Integer> count = new HashMap<>();

            for (PlayerProfile profile : profiles) {
                Set<Map.Entry<String, Integer>> entries = profile.getMiningHistory(ore).getMinesEntry(environment);

                if (!entries.isEmpty()) {
                    profileCount++;

                    for (Map.Entry<String, Integer> entry : entries) {
                        count.put(entry.getKey(), count.getOrDefault(entry.getKey(), 0) + entry.getValue());
                    }
                }
            }
            if (profileCount >= TPS.maximum) {
                int mines = 0;

                for (Map.Entry<String, Integer> entry : count.entrySet()) {
                    mines += entry.getValue() * entry.getValue();
                }
                return Math.sqrt(mines / ((double) count.size()));
            } else {
                return 0.0;
            }
        }
    }

    // Separator

    private final DetectionExecutor detection;

    public XRay(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.detection = new ImplementedDetectionExecutor(this, null, true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        Object[] objects = (Object[]) object;
        MiningHistory miningHistory = (MiningHistory) objects[1];
        World.Environment environment = World.Environment.valueOf(objects[0].toString());
        double minesDeviation = miningHistory.getMinesDeviation(environment);

        if (minesDeviation > 0.0) {
            double averageDeviation = getAverageDeviation(environment, (MiningHistory.MiningOre) objects[2]);

            if (averageDeviation > 0.0) {
                double points = AlgebraUtils.cut(minesDeviation - averageDeviation, 2);

                if (points > 0.0) {
                    this.detection.cancel(
                            "Unfair gathering of " + objects[3] + ", surpassed others by " + points + " " + (points == 1.0 ? "point" : "points")
                    );
                }
            }
        }
    }

}
