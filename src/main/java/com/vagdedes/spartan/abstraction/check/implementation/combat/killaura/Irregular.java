package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.inventory.InventoryView;

public class Irregular extends DetectionExecutor {

    Irregular(CheckExecutor executor) {
        super(executor, "irregular", true);
    }

    void run() {
        if (this.protocol().player.isDead()) {
            cancel("type: irregular, scenario: death");
        } else if (this.protocol().player.isSleeping()) {
            cancel("type: irregular, scenario: sleeping");
        } else {
            InventoryView inventoryView = this.protocol().player.getOpenInventory();

            if (BlockUtils.hasMaterial(inventoryView.getCursor())) {
                cancel("type: irregular, scenario: cursor");
            } else if (inventoryView.countSlots() > PlayerUtils.playerInventorySlots) {
                cancel("type: irregular, scenario: inventory");
            }
        }
    }
}
