package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

public class FastBow extends CheckExecutor {

    public FastBow(SpartanPlayer player) {
        super(HackType.FastBow, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (object instanceof EntityShootBowEvent) {
            if (player.getItemInHand().getType() == Material.BOW) {
                checkBowShots();
                checkBowForce(((EntityShootBowEvent) object).getForce());
            }
        } else if (((PlayerInteractEvent) object).getAction() == Action.RIGHT_CLICK_AIR) {
            PlayerInventory inventory = player.getInventory();

            if (inventory.getItemInHand().getType() == Material.BOW
                    && inventory.contains(Material.ARROW)) {
                this.getTimer().set("interact");
            }
        }
    }

    private void checkBowForce(float force) {
        long time = this.getTimer().get("interact");

        if (!hackType.getCheck().getBooleanOption("check_bow_force", true) || force != 1.0f || time > 500L) {
            return;
        }
        Buffer buffer = this.getBuffer();
        String key = "bow-force";

        if (buffer.start(key, 20, Check.detectionMeasurementTicks) >= 2) {
            buffer.remove(key);
            cancel("type: bow-force, ms: " + time);
        }
    }

    private void checkBowShots() {
        if (hackType.getCheck().getBooleanOption("check_bow_shots", true)) {
            Buffer buffer = this.getBuffer();

            if (buffer.start("bow-shots", 10, Check.detectionMeasurementTicks) >= 0.7) {
                buffer.remove("bow-shots");
                cancel("type: bow-shots");
            }
        }
    }

    @Override
    public void runInternal(boolean cancelled) {

    }

    @Override
    public void schedulerInternal() {

    }

    @Override
    protected boolean canDo() {
        return false;
    }
}
