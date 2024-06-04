package com.vagdedes.spartan.abstraction.check.implementation.inventory;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;

public class InventoryClicks extends CheckExecutor {

    private long time;
    private final Buffer buffer;

    public InventoryClicks(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.buffer = new Buffer(player, new LinkedHashMap<>());
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        InventoryClickEvent event = (InventoryClickEvent) object;
        ItemStack item = event.getCurrentItem();
        ClickType click = event.getClick();

        if (!click.isCreativeAction()
                && !click.isKeyboardClick()
                && item != null
                && item.getItemMeta() != null) {
            Material material = item.getType();

            if (!BlockUtils.isSensitive(player, material)) {
                String key = "";
                boolean quick = player.getOpenInventory().countSlots() <= PlayerUtils.playerInventorySlots,
                        shift = click.isShiftClick();
                String type = BlockUtils.materialToString(material);

                int newHash = type.hashCode();
                int oldHash = buffer.get(key + "hash");
                buffer.set(key + "hash", newHash);

                if (shift
                        && !quick
                        && !BlockUtils.hasMaterial(player.getOpenInventory().getCursor())
                        && buffer.count(key + "shift", 5) == 3) { // Inventory
                    buffer.remove(key + "shift");
                    cancel("type: shift, item: " + type);
                } else {
                    long timePassed = System.currentTimeMillis() - time;
                    time = System.currentTimeMillis();

                    if ((!shift && !quick || newHash != oldHash)) {
                        if (timePassed < 150L) {
                            if (buffer.count("slow", 10) >= 8) {
                                cancel("type: slow, ms: " + timePassed + ", item: " + type);
                            }
                            if (timePassed < 100L) {
                                if (buffer.count("medium", 5) >= 4) {
                                    cancel("type: medium, ms: " + timePassed + ", item: " + type);
                                }
                                if (timePassed < 50L && buffer.count("fast", 5) >= 4) {
                                    cancel("type: fast, ms: " + timePassed + ", item: " + type);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean canRun() {
        return !player.bedrockPlayer;
    }
}
