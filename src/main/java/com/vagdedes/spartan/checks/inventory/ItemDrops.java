package com.vagdedes.spartan.checks.inventory;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.data.Timer;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ItemDrops extends CheckExecutor {

    private static final Enums.HackType check = Enums.HackType.ItemDrops;
    private static final String checkID = Integer.toString(check.ordinal());
    private static final Material water = MaterialUtils.get("water");

    public ItemDrops(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean run() {
        if (canDo()) {
            Timer timer = player.getTimer(check);
            long time = timer.get(checkID);

            if (time <= 50L) {
                Buffer buffer = player.getBuffer(check);

                if (buffer.start(checkID, 10) >= 8) {
                    buffer.remove(checkID);
                    cancel("type: default, ms: " + time, null, 10);
                }
            }
            timer.set(checkID);
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
    public boolean handle(Object object) {
        if (object instanceof String) {
            String[] args = object.toString().split(" ");

            if (!args[0].equalsIgnoreCase("/" + Register.plugin.getName())) {
                for (String arg : args) {
                    SpartanPlayer p = SpartanBukkit.getPlayer(arg);

                    if (p != null) {
                        check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), 5);
                    }
                }
            }
        } else {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                    && canDo()
                    && e.getClickedBlock().getType() == water
                    && player.getItemInHand().getType() == Material.GLASS_BOTTLE) {
                check.getCheck().addDisabledUser(player.getUniqueId(), check.toString(), 10);
            }
        }
        return false;
    }
}
