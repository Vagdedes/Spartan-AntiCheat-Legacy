package me.vagdedes.spartan.checks.implementation.inventory;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class InventoryClicks {

    private static final HackType check = HackType.InventoryClicks;

    public static void run(SpartanPlayer p, ItemStack item, ClickType click) {
        if (!p.canRunCheck(check, true)
                || click.isCreativeAction() || click.isKeyboardClick()
                || item == null || item.getItemMeta() == null
                || Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()) {
            return;
        }
        Material material = item.getType();

        if (BlockUtils.isSensitive(p, material)) {
            return;
        }
        Buffer buffer = p.getBuffer(check);
        String key = "";
        boolean quick = !PlayerData.isUsingAnInventory(p, 8);
        boolean shift = click.isShiftClick();
        String type = BlockUtils.materialToString(material);

        int newHash = type.hashCode();
        int oldHash = p.getBuffer(check).get(key + "hash");
        buffer.set(key + "hash", newHash);

        if (shift && !quick && !PlayerData.isUsingTheCursor(p) && p.getViolations(check).hasLevel()
                && buffer.start(key + "shift", 5) > Math.min(10 + LagLeniencies.getDelay(p), 20)) { // Inventory
            buffer.remove(key + "shift");
            new HackPrevention(p, check, "type: shift, item: " + type);
        } else {
            Timer timer = p.getTimer(check);
            long time = timer.get(key + "time");

            if (timer.has(time) && (!shift && !quick || newHash != oldHash)) {
                if (time < 50L) {
                    key += "fast";

                    if (buffer.start(key, 5) >= (time == 0L ? 4 : 2)) {
                        buffer.remove(key);
                        new HackPrevention(p, check, "type: fast, ms: " + time + ", item: " + type);
                    }
                } else if (time < 100L) {
                    key += "average";

                    if (buffer.start(key, 5) >= 4) {
                        buffer.remove(key);
                        new HackPrevention(p, check, "type: average, ms: " + time + ", item: " + type);
                    }
                } else if (time < 150L) {
                    key += "slow";

                    if (buffer.start(key, 10) >= 8) {
                        buffer.remove(key);
                        new HackPrevention(p, check, "type: slow, ms: " + time + ", item: " + type);
                    }
                }
            }
        }
        p.getTimer(check).set(key + "time");
    }
}
