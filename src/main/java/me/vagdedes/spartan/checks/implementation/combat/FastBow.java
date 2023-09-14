package me.vagdedes.spartan.checks.implementation.combat;

import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.replicates.SpartanInventory;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class FastBow {

    private static final HackType check = HackType.FastBow;

    public static void run(SpartanPlayer p, float force) {
        if (!p.canRunCheck(check, true) || p.getItemInHand().getType() != Material.BOW) {
            return;
        }
        checkBowShots(p);
        checkBowForce(p, force);
    }

    public static void runInteract(SpartanPlayer p, Action action) {
        if (action == Action.RIGHT_CLICK_AIR && p.canRunCheck(check, false)) {
            SpartanInventory inventory = p.getInventory();
            ItemStack item = inventory.getItemInHand();

            if (item.getType() == Material.BOW && inventory.contains(Material.ARROW)) {
                p.getTimer(check).set("interact");
            }
        }
    }

    private static void checkBowForce(SpartanPlayer p, float force) {
        long time = p.getTimer(check).get("interact");

        if (!check.getCheck().getBooleanOption("check_bow_force", true) || force != 1.0f || time > 500L) {
            return;
        }
        Buffer buffer = p.getBuffer(check);
        String key = "bow-force";

        if (buffer.start(key, 20) >= 2) {
            buffer.remove(key);
            new HackPrevention(p, check, "type: bow-force, ms: " + time);
        }
    }

    private static void checkBowShots(SpartanPlayer p) {
        if (check.getCheck().getBooleanOption("check_bow_shots", true)) {
            String cancel = null;
            Buffer buffer = p.getBuffer(check);
            String key = "bow-shots=";
            int delay = AlgebraUtils.integerRound(LagLeniencies.getDelay(p));
            int[] details = new int[]{10, 6};

            if (buffer.start(key + "fast", details[0]) == Math.min(details[1] + delay, details[0])) { // Combat
                buffer.remove(key + "fast");
                cancel = "fast";
            } else {
                details = new int[]{20, 7};

                if (buffer.start(key + "medium", details[0]) == Math.min(details[1] + delay, details[0])) { // Combat
                    buffer.remove(key + "medium");
                    cancel = "medium";
                } else {
                    details = new int[]{30, 9};

                    if (buffer.start(key + "slow", details[0]) == Math.min(details[1] + delay, details[0])) { // Combat
                        buffer.remove(key + "slow");
                        cancel = "slow";
                    }
                }
            }

            if (cancel != null) {
                new HackPrevention(p, check, "type: bow-shots(" + cancel + ")");
            }
        }
    }
}
