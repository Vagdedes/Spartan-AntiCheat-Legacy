package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEvidence {

    private final Map<Enums.HackType, String> live, historical, informational; // Live object is used for synchronization

    PlayerEvidence() {
        this.live = new ConcurrentHashMap<>(2, 1.0f);
        this.historical = new ConcurrentHashMap<>(2, 1.0f);
        this.informational = new ConcurrentHashMap<>(2, 1.0f);
    }

    // Separator

    public void clear(boolean live,
                      boolean historical,
                      boolean informational) {
        if (live) {
            this.live.clear();
        }
        if (historical) {
            this.historical.clear();
        }
        if (informational) {
            this.informational.clear();
        }
    }

    public void remove(Enums.HackType hackType,
                       boolean live,
                       boolean historical,
                       boolean informational) {
        if (live) {
            this.live.remove(hackType);
        }
        if (historical) {
            this.historical.remove(hackType);
        }
        if (informational) {
            this.informational.remove(hackType);
        }
    }

    public void add(Enums.HackType hackType, String info,
                    boolean live,
                    boolean historical,
                    boolean informational) {
        if (live) {
            this.live.put(hackType, info);
        }
        if (historical) {
            this.historical.put(hackType, info);
        }
        if (informational) {
            this.informational.put(hackType, info);
        }
    }

    // Separator

    public Collection<Enums.HackType> getKnowledgeList(boolean informational) {
        Collection<Enums.HackType> set = new HashSet<>(live.keySet());
        set.addAll(historical.keySet());

        if (informational) {
            set.addAll(this.informational.keySet());
        }
        return set;
    }

    // Separator

    public String getKnowledge(Enums.HackType hackType, String color, boolean informational) {
        String knowledge = historical.get(hackType);

        if (knowledge == null) {
            knowledge = live.get(hackType);

            if (knowledge == null && informational) {
                knowledge = this.informational.get(hackType);
            }
        }
        return knowledge == null ? null : addColor(knowledge, color);
    }

    private String addColor(String knowledge, String color) {
        return knowledge.replace("§r", "§r" + color);
    }

    // Separator

    public boolean has(Enums.HackType hackType, boolean informational) {
        return live.containsKey(hackType)
                || historical.containsKey(hackType)
                || informational && this.informational.containsKey(hackType);
    }

    // Separator

    public Collection<Enums.HackType> calculate(SpartanPlayer player, PlayerViolation playerViolation) {
        if (this.live.containsKey(playerViolation.hackType)
                || this.historical.containsKey(playerViolation.hackType)) {
            return this.getKnowledgeList(false);
        }
        Check check = playerViolation.hackType.getCheck();

        if (check.supportsLiveEvidence) {
            double violationCount = player.getViolations(playerViolation.hackType).getLevel(playerViolation.identity)
                    - AlgebraUtils.integerCeil(Latency.getDelay(player))
                    - playerViolation.getIgnoredViolations(player);

            if (violationCount > 0.0) {
                double ratio = violationCount / Check.standardIgnoredViolations;

                if (ratio >= Check.standardIgnoredViolations) {
                    this.live.put(playerViolation.hackType,
                            "Ratio: " + AlgebraUtils.cut(ratio, 2) + "%"
                    );
                    InteractiveInventory.playerInfo.refresh(player.name);
                    MainMenu.refresh();
                    return this.getKnowledgeList(false);
                }
            }
        }
        return new ArrayList<>(0);
    }

}
