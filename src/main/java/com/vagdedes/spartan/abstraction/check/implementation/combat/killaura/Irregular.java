package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.inventory.InventoryView;

public class Irregular extends HardcodedDetection {

    Irregular(CheckRunner executor) {
        super(executor, null, null, "irregular", true);
    }

    void run() {
        this.call(() -> {
            if (this.protocol.bukkitExtra.isDead()) {
                this.setHackingRatio(1.0);
                cancel("irregular, scenario: death");
            } else if (this.protocol.bukkit().isSleeping()) {
                this.setHackingRatio(1.0);
                cancel("irregular, scenario: sleeping");
            } else {
                InventoryView inventoryView = this.protocol.bukkit().getOpenInventory();

                if (BlockUtils.hasMaterial(inventoryView.getCursor())) {
                    this.setHackingRatio(1.0);
                    cancel("irregular, scenario: cursor");
                } else if (inventoryView.countSlots() > PlayerUtils.playerInventorySlots) {
                    this.setHackingRatio(1.0);
                    cancel("irregular, scenario: inventory");
                } else {
                    this.setHackingRatio(0.0);
                }
            }
        });
    }

}
