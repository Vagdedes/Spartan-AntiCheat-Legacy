package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.inventory.InventoryView;

public class Irregular extends CheckDetection {

    Irregular(CheckRunner executor) {
        super(
                executor,
                null,
                null,
                "irregular",
                true,
                1L,
                1L,
                1L,
                1L
        );
    }

    void run() {
        this.call(() -> {
            if (this.protocol.bukkitExtra.isDead()) {
                cancel("irregular, scenario: death");
            } else if (this.protocol.bukkit().isSleeping()) {
                cancel("irregular, scenario: sleeping");
            } else {
                InventoryView inventoryView = this.protocol.bukkit().getOpenInventory();

                if (BlockUtils.hasMaterial(inventoryView.getCursor())) {
                    cancel("irregular, scenario: cursor");
                } else if (inventoryView.countSlots() > PlayerUtils.playerInventorySlots) {
                    cancel("irregular, scenario: inventory");
                }
            }
        });
    }

}
