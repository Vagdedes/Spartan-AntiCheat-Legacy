package com.vagdedes.spartan.checks.combat;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.replicates.SpartanInventory;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class FastBow extends CheckExecutor { // todo apply buffer idea

    public FastBow(SpartanPlayer player) {
        super(HackType.FastBow, player);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
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
        return false;
    }

    private void checkBowForce(float force) {
        long time = player.getTimer(hackType).get("interact");

        if (!hackType.getCheck().getBooleanOption("check_bow_force", true) || force != 1.0f || time > 500L) {
            return;
        }
        Buffer buffer = player.getBuffer(hackType);
        String key = "bow-force";

        if (buffer.start(key, 20) >= 2) {
            buffer.remove(key);
            cancel("type: bow-force, ms: " + time);
        }
    }

    private void checkBowShots() {
        if (hackType.getCheck().getBooleanOption("check_bow_shots", true)) {
            String key = "bow-shots=";
            Buffer buffer = player.getBuffer(hackType);
            String[] array = new String[3];
            boolean run = false;

            if (buffer.start(key + "fast", 10) == 7) {
                buffer.remove(key + "fast");
                array[0] = "fast";
                run = true;
            }
            if (buffer.start(key + "medium", 20) == 13) {
                buffer.remove(key + "medium");
                array[1] = "medium";
                run = true;
            }
            if (buffer.start(key + "slow", 30) == 20) {
                buffer.remove(key + "slow");
                array[2] = "slow";
                run = true;
            }

            if (run) {
                cancel("type: bow-shots, level/s: " + StringUtils.toString(array, ", "));
            }
        }
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, false);
    }
}
