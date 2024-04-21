package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Handlers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class TridentUse {

    private static final int riptideMaxSafeLevel = 3;

    public static void run(SpartanPlayer p) {
        for (ItemStack item : new ItemStack[]{p.getItemInHand(), p.getInventory().itemInOffHand}) {
            if (item.getType() == Material.TRIDENT) {
                int level = item.getEnchantmentLevel(Enchantment.RIPTIDE);

                if (level > 0) {
                    int ticks = AlgebraUtils.integerRound(Math.log(level) * TPS.maximum);

                    if (level > riptideMaxSafeLevel) {
                        p.getHandlers().add(Handlers.HandlerType.AbstractVelocity, ticks);
                    } else {
                        p.getHandlers().add(Handlers.HandlerType.Trident, ticks);
                    }
                    break;
                }
            }
        }
    }
}
