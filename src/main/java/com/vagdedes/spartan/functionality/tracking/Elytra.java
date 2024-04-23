package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.check.implementation.exploits.Exploits;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Elytra {

    public static void judge(SpartanPlayer p, boolean gliding, boolean event) {
        if (p.getVehicle() == null) {
            ItemStack i = p.getInventory().getChestplate();

            if (i != null) {
                if (i.getType() == Material.ELYTRA) {
                    if (i.getDurability() < 432) {
                        if (!event
                                || !p.getExecutor(Enums.HackType.Exploits).handle(false, Exploits.ELYTRA_GLIDE_SPAM)) {
                            p.movement.setGliding(gliding, false);
                            Trackers handlers = p.getTrackers();
                            handlers.add(Trackers.TrackerType.ElytraWear, 2);

                            if (gliding) {
                                handlers.add(Trackers.TrackerType.ElytraUse, (int) (TPS.maximum / 2));
                            }
                        }
                    } else {
                        p.movement.setGliding(false, gliding);

                        if (gliding) {
                            p.getExecutor(Enums.HackType.Exploits).handle(false, new int[]{2, 2});
                        }
                    }
                } else {
                    p.movement.setGliding(false, gliding);
                }
            } else {
                p.movement.setGliding(false, gliding);

                if (gliding && !p.getTrackers().has(Trackers.TrackerType.ElytraWear)) {
                    p.getExecutor(Enums.HackType.Exploits).handle(false, new int[]{3, 2});
                }
            }
        } else {
            p.movement.setGliding(false, gliding);

            if (gliding) {
                p.getExecutor(Enums.HackType.Exploits).handle(false, new int[]{1, 0});
            }
        }
    }

}
