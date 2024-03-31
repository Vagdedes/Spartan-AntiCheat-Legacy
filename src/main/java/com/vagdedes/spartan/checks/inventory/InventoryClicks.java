package com.vagdedes.spartan.checks.inventory;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClicks extends CheckExecutor {

    private static final Enums.HackType check = Enums.HackType.InventoryClicks;

    public InventoryClicks(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        InventoryClickEvent event = (InventoryClickEvent) object;
        ItemStack item = event.getCurrentItem();
        ClickType click = event.getClick();

        if (canDo()
                && !click.isCreativeAction()
                && !click.isKeyboardClick()
                && item != null
                && item.getItemMeta() != null) {
            Material material = item.getType();

            if (!BlockUtils.isSensitive(player, material)) {
                Buffer buffer = player.getBuffer(check);
                String key = "";
                boolean quick = !PlayerUtils.isUsingAnInventory(player, 8);
                boolean shift = click.isShiftClick();
                String type = BlockUtils.materialToString(material);

                int newHash = type.hashCode();
                int oldHash = player.getBuffer(check).get(key + "hash");
                buffer.set(key + "hash", newHash);

                if (shift
                        && !quick
                        && !player.getOpenInventory().isUsingTheCursor()
                        && buffer.start(key + "shift", 5) > 10) { // Inventory
                    buffer.remove(key + "shift");
                    cancel("type: shift, item: " + type);
                } else {
                    Timer timer = player.getTimer(check);
                    long time = timer.get(key + "time");

                    if (timer.has(time)
                            && (!shift && !quick || newHash != oldHash)) {
                        if (time < 150L) {
                            if (buffer.start("slow", 10) >= 8) {
                                buffer.remove("slow");
                                cancel("type: slow, ms: " + time + ", item: " + type);
                            }
                            if (time < 100L) {
                                if (buffer.start("average", 5) >= 4) {
                                    buffer.remove("average");
                                    cancel("type: average, ms: " + time + ", item: " + type);
                                }
                                if (time < 50L && buffer.start("fast", 5) >= (time == 0L ? 4 : 2)) {
                                    buffer.remove("fast");
                                    cancel("type: fast, ms: " + time + ", item: " + type);
                                }
                            }
                        }
                    }
                }
                player.getTimer(check).set(key + "time");
            }
        }
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(check, true)
                && !Compatibility.CompatibilityType.MajorIncompatibility.isFunctional();
    }
}
