package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.inventory.InventoryView;

public class Irregular extends ProbabilityDetection {

    Irregular(CheckRunner executor) {
        super(executor, "irregular", true);
    }

    final void run() {
        if (this.protocol().spartan.isDead()) {
            cancel("type: irregular, scenario: death");
        } else if (this.protocol().bukkit.isSleeping()) {
            cancel("type: irregular, scenario: sleeping");
        } else {
            InventoryView inventoryView = this.protocol().bukkit.getOpenInventory();

            if (BlockUtils.hasMaterial(inventoryView.getCursor())) {
                cancel("type: irregular, scenario: cursor");
            } else if (inventoryView.countSlots() > PlayerUtils.playerInventorySlots) {
                cancel("type: irregular, scenario: inventory");
            }
        }
    }

}
