package com.vagdedes.spartan.abstraction.check.implementation.inventory;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClicks extends CheckExecutor {

    private final DetectionExecutor
            shift,
            slow,
            medium,
            fast;
    private long time;
    private int oldHash;
    private final Buffer.IndividualBuffer
            slowBuffer,
            mediumBuffer,
            fastBuffer,
            shiftBuffer;

    public InventoryClicks(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.shift = new ImplementedDetectionExecutor(this, "shift", true);
        this.slow = new ImplementedDetectionExecutor(this, "slow", true);
        this.medium = new ImplementedDetectionExecutor(this, "medium", true);
        this.fast = new ImplementedDetectionExecutor(this, "fast", true);
        this.slowBuffer = new Buffer.IndividualBuffer();
        this.mediumBuffer = new Buffer.IndividualBuffer();
        this.fastBuffer = new Buffer.IndividualBuffer();
        this.shiftBuffer = new Buffer.IndividualBuffer();
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
                boolean quick = player.getInstance().getOpenInventory().countSlots() <= PlayerUtils.playerInventorySlots,
                        shift = click.isShiftClick();
                String type = BlockUtils.materialToString(material);

                int newHash = type.hashCode(),
                        oldHash = this.oldHash;
                this.oldHash = newHash;

                if (shift
                        && !quick
                        && !BlockUtils.hasMaterial(player.getInstance().getOpenInventory().getCursor())
                        && shiftBuffer.count(1, 5) == 4) { // Inventory
                    shiftBuffer.reset();
                    this.shift.cancel("type: shift, item: " + type);
                } else {
                    long timePassed = System.currentTimeMillis() - time;
                    time = System.currentTimeMillis();

                    if ((!shift && !quick || newHash != oldHash)) {
                        if (timePassed < 150L) {
                            if (slowBuffer.count(1, 10) >= 8) {
                                this.slow.cancel("type: slow, ms: " + timePassed + ", item: " + type);
                            }
                            if (timePassed < 100L) {
                                if (mediumBuffer.count(1, 5) >= 4) {
                                    this.medium.cancel("type: medium, ms: " + timePassed + ", item: " + type);
                                }
                                if (timePassed < 50L && fastBuffer.count(1, 5) >= 4) {
                                    this.fast.cancel("type: fast, ms: " + timePassed + ", item: " + type);
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
        return !player.isBedrockPlayer();
    }
}
