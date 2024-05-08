package com.vagdedes.spartan.abstraction.check.implementation.combat;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.replicates.SpartanInventory;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class FastBow extends CheckExecutor {

    public FastBow(SpartanPlayer player) {
        super(HackType.FastBow, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (object instanceof EntityShootBowEvent) {
            if (canDo()
                    && player.getItemInHand().getType() == Material.BOW) {
                checkBowShots();
                checkBowForce(((EntityShootBowEvent) object).getForce());
            }
        } else if (canDo()
                && ((PlayerInteractEvent) object).getAction() == Action.RIGHT_CLICK_AIR) {
            SpartanInventory inventory = player.getInventory();

            if (inventory.itemInHand.getType() == Material.BOW
                    && inventory.contains(Material.ARROW)) {
                player.getTimer(hackType).set("interact");
            }
        }
    }

    private void checkBowForce(float force) {
        long time = player.getTimer(hackType).get("interact");

        if (!hackType.getCheck().getBooleanOption("check_bow_force", true) || force != 1.0f || time > 500L) {
            return;
        }
        Buffer buffer = player.getBuffer(hackType);
        String key = "bow-force";

        if (buffer.start(key, 20, Check.detectionMeasurementTicks) >= 2) {
            buffer.remove(key);
            cancel("type: bow-force, ms: " + time);
        }
    }

    private void checkBowShots() {
        if (hackType.getCheck().getBooleanOption("check_bow_shots", true)) {
            Buffer buffer = player.getBuffer(hackType);

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
    public void scheduler() {

    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, false);
    }
}
