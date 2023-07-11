package me.vagdedes.spartan.checks.inventory;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.event.block.Action;

public class ItemDrops {

    private static final HackType check = HackType.ItemDrops;
    private static final String checkID = Integer.toString(check.ordinal());
    private static final Material water = MaterialUtils.get("water");

    public static void run(SpartanPlayer p) {
        if (canDo(p)) {
            Timer timer = p.getTimer(check);
            long time = timer.get(checkID);

            if (time <= 50L) {
                Buffer buffer = p.getBuffer(check);

                if (buffer.start(checkID, 10) >= 8) {
                    buffer.remove(checkID);
                    new HackPrevention(p, check, "type: default, ms: " + time, 10);
                }
            }
            timer.set(checkID);
        }
    }

    public static void runInteract(SpartanPlayer p, SpartanBlock b, Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK
                && canDo(p)
                && b.getType() == water
                && p.getItemInHand().getType() == Material.GLASS_BOTTLE) {
            check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), 10);
        }
    }

    public static void runCommand(String s) {
        String[] args = s.split(" ");

        if (!args[0].equalsIgnoreCase("/" + Register.plugin.getName())) {
            for (String arg : args) {
                SpartanPlayer p = SpartanBukkit.getPlayer(arg);

                if (p != null) {
                    check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), 5);
                }
            }
        }
    }

    private static boolean canDo(SpartanPlayer p) {
        return !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && p.canRunCheck(check, true) && !p.isDead();
    }
}
