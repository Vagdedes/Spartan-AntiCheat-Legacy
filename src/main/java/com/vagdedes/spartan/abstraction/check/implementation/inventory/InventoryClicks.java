package com.vagdedes.spartan.abstraction.check.implementation.inventory;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClicks extends CheckExecutor {

    public InventoryClicks(SpartanPlayer player) {
        super(Enums.HackType.InventoryClicks, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        InventoryClickEvent event = (InventoryClickEvent) object;
        ItemStack item = event.getCurrentItem();
        ClickType click = event.getClick();

        if (!click.isCreativeAction()
                && !click.isKeyboardClick()
                && item != null
                && item.getItemMeta() != null) {
            Material material = item.getType();

            if (!BlockUtils.isSensitive(player, material)) {
                Buffer buffer = this.getBuffer();
                String key = "";
                boolean quick = player.getOpenInventory().countSlots() <= PlayerUtils.playerInventorySlots,
                        shift = click.isShiftClick();
                String type = BlockUtils.materialToString(material);

                int newHash = type.hashCode();
                int oldHash = this.getBuffer().get(key + "hash");
                buffer.set(key + "hash", newHash);

                if (shift
                        && !quick
                        && !BlockUtils.hasMaterial(player.getOpenInventory().getCursor())
                        && buffer.count(key + "shift", 5) == 3) { // Inventory
                    buffer.remove(key + "shift");
                    cancel("type: shift, item: " + type);
                } else {
                    Timer timer = this.getTimer();
                    long time = timer.get(key + "time");

                    if (timer.has(time)
                            && (!shift && !quick || newHash != oldHash)) {
                        if (time < 150L) {
                            if (buffer.count("slow", 10) == 8) {
                                buffer.remove("slow");
                                cancel("type: slow, ms: " + time + ", item: " + type);
                            }
                            if (time < 100L) {
                                if (buffer.count("average", 5) == 4) {
                                    buffer.remove("average");
                                    cancel("type: average, ms: " + time + ", item: " + type);
                                }
                                if (time == 0L || time < 50L && buffer.count("fast", 5) == 4) {
                                    buffer.remove("fast");
                                    cancel("type: fast, ms: " + time + ", item: " + type);
                                }
                            }
                        }
                    }
                }
                this.getTimer().set(key + "time");
            }
        }
    }

    @Override
    protected boolean canRun() {
        return !player.bedrockPlayer;
    }
}
