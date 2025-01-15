package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

public class FastBow extends CheckRunner {

    private final CheckDetection
            bowForceDetection,
            bowShotsDetection;
    private long interactTime;
    private final Buffer.IndividualBuffer bowForce, bowShots;

    public FastBow(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.bowForceDetection = new ImplementedProbabilityDetection(this, null, null, "bow_force", true);
        this.bowShotsDetection = new ImplementedProbabilityDetection(this, null, null, "bow_shots", true);
        this.bowForce = new Buffer.IndividualBuffer();
        this.bowShots = new Buffer.IndividualBuffer();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof EntityShootBowEvent) {
            if (this.protocol.spartan.getItemInHand().getType() == Material.BOW) {
                checkBowShots();
                checkBowForce(((EntityShootBowEvent) object).getForce());
            }
        } else if (((PlayerInteractEvent) object).getAction() == Action.RIGHT_CLICK_AIR) {
            PlayerInventory inventory = this.protocol.bukkit().getInventory();

            if (inventory.getItemInHand().getType() == Material.BOW
                    && inventory.contains(Material.ARROW)) {
                this.interactTime = System.currentTimeMillis();
            }
        }
    }

    private void checkBowForce(float force) {
        this.bowForceDetection.call(() -> {
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
                        "bow-force, ms: " + timePassed
                );
            }
        });
    }

    private void checkBowShots() {
        this.bowShotsDetection.call(() -> {
            int buffer = bowShots.count(1, 10),
                    threshold = 7;
            if (buffer >= threshold) {
                this.bowShotsDetection.cancel(
                        "bow-shots"
                );
            }
        });
    }

}
