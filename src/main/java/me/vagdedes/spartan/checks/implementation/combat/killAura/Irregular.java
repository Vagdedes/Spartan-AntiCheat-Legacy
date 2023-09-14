package me.vagdedes.spartan.checks.implementation.combat.killAura;

// Study of hits that couldn't have been dealt

import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.PlayerData;

public class Irregular {

    static void run(SpartanPlayer player) {
        if (player.isDead()) {
            KillAura.violate(player, "type: irregular(death)");
        } else if (player.isSleeping()) {
            KillAura.violate(player, "type: irregular(sleeping)");
        } else if (PlayerData.isUsingTheCursor(player)) {
            KillAura.violate(player, "type: irregular(cursor)");
        } else if (PlayerData.isUsingAnInventory(player, 8)) {
            KillAura.violate(player, "type: irregular(inventory)");
        }
    }
}
