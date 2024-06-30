package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

public class FastBow extends CheckExecutor {

    private long interactTime;
    private final Buffer.IndividualBuffer bowForce, bowShots;

    public FastBow(HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, false);
        this.bowForce = new Buffer.IndividualBuffer(player);
        this.bowShots = new Buffer.IndividualBuffer(player);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof EntityShootBowEvent) {
            if (player.getItemInHand().getType() == Material.BOW) {
                checkBowShots();
                checkBowForce(((EntityShootBowEvent) object).getForce());
            }
        } else if (((PlayerInteractEvent) object).getAction() == Action.RIGHT_CLICK_AIR) {
            PlayerInventory inventory = player.getInventory();

            if (inventory.getItemInHand().getType() == Material.BOW
                    && inventory.contains(Material.ARROW)) {
                this.interactTime = System.currentTimeMillis();
            }
        }
    }

    private void checkBowForce(float force) {
        long timePassed = System.currentTimeMillis() - interactTime;

        if (!hackType.getCheck().getBooleanOption("check_bow_force", true)
                || force != 1.0f
                || timePassed > 500L) {
            return;
        }
        if (bowForce.count(1, 20) == 2) {
            bowForce.reset();
            cancel("type: bow-force, ms: " + timePassed);
        }
    }

    private void checkBowShots() {
        if (hackType.getCheck().getBooleanOption("check_bow_shots", true)
                && bowShots.count(1, 10) == 7) {
            bowShots.reset();
            cancel("type: bow-shots");
        }
    }

}
