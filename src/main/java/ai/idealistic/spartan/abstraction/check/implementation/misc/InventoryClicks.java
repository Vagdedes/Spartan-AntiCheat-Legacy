package ai.idealistic.spartan.abstraction.check.implementation.misc;

import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.data.Buffer;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.utils.minecraft.entity.PlayerUtils;
import ai.idealistic.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClicks extends CheckRunner {

    private final CheckDetection
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

    public InventoryClicks(CheckEnums.HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.shift = new ImplementedDetection(this, Check.DataType.JAVA, null, "shift", true);
        this.slow = new ImplementedDetection(this, Check.DataType.JAVA, null, "slow", true);
        this.medium = new ImplementedDetection(this, Check.DataType.JAVA, null, "medium", true);
        this.fast = new ImplementedDetection(this, Check.DataType.JAVA, null, "fast", true);
        this.slowBuffer = new Buffer.IndividualBuffer();
        this.mediumBuffer = new Buffer.IndividualBuffer();
        this.fastBuffer = new Buffer.IndividualBuffer();
        this.shiftBuffer = new Buffer.IndividualBuffer();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof InventoryClickEvent) {
            if (this.shift.canCall()
                    || this.slow.canCall()
                    || this.medium.canCall()
                    || this.fast.canCall()) {
                InventoryClickEvent event = (InventoryClickEvent) object;
                ItemStack item = event.getCurrentItem();
                ClickType click = event.getClick();

                // EXTENSION: Removed item.getItemMeta() != null bypass.
                // We now check all items, regardless of enchantments or custom names.
                if (!click.isCreativeAction() && item != null) {
                    Material material = item.getType();

                    if (material != Material.AIR && !BlockUtils.isSensitive(this.protocol, material)) {
                        int slots = this.protocol.getOpenInventoryCountSlots(PlayerUtils.playerInventorySlots + 1);
                        boolean quick = slots <= PlayerUtils.playerInventorySlots;
                        boolean shift = click.isShiftClick();

                        // EXTENSION: Track Keyboard clicks to catch Auto-Armor and Auto-Refill
                        boolean hotkey = click.isKeyboardClick();

                        String type = BlockUtils.materialToString(material);

                        int newHash = type.hashCode();
                        int oldHash = this.oldHash;
                        this.oldHash = newHash;

                        long timePassed = System.currentTimeMillis() - time;
                        time = System.currentTimeMillis();

                        // EXTENSION: Protect against division-by-zero lag spikes
                        long safeTime = Math.max(timePassed, 1L);

                        // Detection 1: Auto-Armor / Hotkey Macros
                        if (hotkey && safeTime < 50L) {
                            if (shiftBuffer.count(1.5, 10) >= 5) { // Humans cannot hit 5 different number keys in 50ms
                                this.shift.call(() -> this.shift.cancel("auto-armor/hotkey, ms: " + timePassed + ", item: " + type));
                            }
                        }
                        // Detection 2: Shift-Click Macros
                        else if (shift
                                && !quick
                                && !BlockUtils.hasMaterial(this.protocol.getOpenInventoryCursor())) {

                            if (safeTime < 60L) {
                                // EXTENSION: Fixed the exact-match '== 4' bypass by using '>='
                                if (shiftBuffer.count(1, 10) >= 5) {
                                    shiftBuffer.reset();
                                    this.shift.call(() -> this.shift.cancel("shift-macro, ms: " + timePassed + ", item: " + type));
                                }
                            } else {
                                shiftBuffer.decrease(0.5);
                            }
                        }
                        // Detection 3: Standard Fast-Clicking (Auto-Clicker in GUI)
                        else {
                            if ((!shift && !quick) || newHash != oldHash) {
                                if (timePassed < 150L) {

                                    // EXTENSION: Dynamic amplitude cap.
                                    // A 0ms lag spike applies max 3.0 points, preventing instant false bans.
                                    double amplitude = Math.min(100.0 / (double) safeTime, 3.0);

                                    this.slow.call(() -> {
                                        if (slowBuffer.count(amplitude * 0.5, 20) >= 12) {
                                            this.slow.cancel("slow, ms: " + timePassed + ", item: " + type);
                                        }
                                    });
                                    if (timePassed < 100L) {
                                        this.medium.call(() -> {
                                            if (mediumBuffer.count(amplitude * 0.75, 15) >= 8) {
                                                this.medium.cancel("medium, ms: " + timePassed + ", item: " + type);
                                            }
                                        });
                                        this.fast.call(() -> {
                                            if (timePassed < 50L && fastBuffer.count(amplitude, 10) >= 6) {
                                                this.fast.cancel("fast, ms: " + timePassed + ", item: " + type);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                }

                if (this.prevent()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}