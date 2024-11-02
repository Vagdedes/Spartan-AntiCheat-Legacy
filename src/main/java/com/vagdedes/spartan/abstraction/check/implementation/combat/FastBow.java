package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

public class FastBow extends CheckExecutor {

    private final DetectionExecutor
            bowForceDetection,
            bowShotsDetection;
    private long interactTime;
    private final Buffer.IndividualBuffer bowForce, bowShots;

    public FastBow(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.bowForceDetection = new ImplementedDetectionExecutor(this, "bow_force", true);
        this.bowShotsDetection = new ImplementedDetectionExecutor(this, "bow_shots", true);
        this.bowForce = new Buffer.IndividualBuffer();
        this.bowShots = new Buffer.IndividualBuffer();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof EntityShootBowEvent) {
            if (player.getItemInHand().getType() == Material.BOW) {
                checkBowShots();
                checkBowForce(((EntityShootBowEvent) object).getForce());
            }
        } else if (((PlayerInteractEvent) object).getAction() == Action.RIGHT_CLICK_AIR) {
            PlayerInventory inventory = player.getInstance().getInventory();

            if (inventory.getItemInHand().getType() == Material.BOW
                    && inventory.contains(Material.ARROW)) {
                this.interactTime = System.currentTimeMillis();
            }
        }
    }

    private void checkBowForce(float force) {
        if (force != 1.0f) {
            return;
        }
        long timePassed = System.currentTimeMillis() - interactTime;

        if (timePassed > 500L) {
            return;
        }
        int buffer = bowForce.count(1, 20),
                threshold = 2;

        if (buffer >= threshold) {
            bowForce.reset();
            this.bowForceDetection.cancel(
                    "type: bow-force, ms: " + timePassed,
                    buffer / (double) threshold
            );
        }
    }

    private void checkBowShots() {
        int buffer = bowShots.count(1, 10),
                threshold = 7;
        if (buffer >= threshold) {
            this.bowShotsDetection.cancel(
                    "type: bow-shots",
                    buffer / (double) threshold
            );
        }
    }

}
