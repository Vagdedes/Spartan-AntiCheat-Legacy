package com.vagdedes.spartan.checks.inventory;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ItemDrops extends CheckExecutor {

    private static final Enums.HackType check = Enums.HackType.ItemDrops;

    public ItemDrops(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (canDo()) {
            Timer timer = player.getTimer(check);
            long time = timer.get("");

            if (time <= 50L) {
                Buffer buffer = player.getBuffer(check);
                int minimum = 10, maximum = 30;
                double ratio = 0.7;

                if (buffer.ratioStart("", minimum, maximum) >= ratio) {
                    buffer.remove("");
                    cancel("type: default"
                            + ", ms: " + time
                            + ", minimum-ticks: " + minimum
                            + ", maximum-ticks: " + maximum
                            + ", remaining-ticks: " + buffer.getRemainingTicks("bow-shots")
                            + ", ratio: " + ratio, null, 10);
                }
            }
            timer.set("");
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                && player.canRunCheck(check, true)
                && !player.isDead();
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object instanceof String) {
            String[] args = object.toString().split(" ");

            if (!args[0].equalsIgnoreCase("/" + Register.plugin.getName())) {
                for (String arg : args) {
                    SpartanPlayer p = SpartanBukkit.getPlayer(arg);

                    if (p != null) {
                        player.getViolations(hackType).addDisableCause(check.toString(), null, 5);
                    }
                }
            }
        } else {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                    && canDo()
                    && e.getClickedBlock().getType() == BlockUtils.water
                    && player.getItemInHand().getType() == Material.GLASS_BOTTLE) {
                player.getViolations(hackType).addDisableCause(check.toString(), null, 10);
            }
        }
        return false;
    }
}
