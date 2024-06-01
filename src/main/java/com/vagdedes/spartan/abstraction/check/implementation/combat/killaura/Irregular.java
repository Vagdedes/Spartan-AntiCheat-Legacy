package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import org.bukkit.inventory.InventoryView;

public class Irregular extends DetectionExecutor {

    Irregular(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void runDamage() {
        if (player.isDead()) {
            cancel("type: irregular, scenario: death");
        } else if (player.isSleeping()) {
            cancel("type: irregular, scenario: sleeping");
        } else {
            InventoryView inventoryView = player.getOpenInventory();

            if (inventoryView != null) {
                if (BlockUtils.hasMaterial(inventoryView.getCursor())) {
                    cancel("type: irregular, scenario: cursor");
                } else if (inventoryView.countSlots() > PlayerUtils.playerInventorySlots) {
                    cancel("type: irregular, scenario: inventory");
                }
            }
        }
    }
}
