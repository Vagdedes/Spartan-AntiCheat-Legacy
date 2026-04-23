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
    private int lastSlot = -1; // EXTENSION: State tracking for Slot Deltas
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

                if (!click.isCreativeAction() && item != null) {
                    Material material = item.getType();

                    if (material != Material.AIR && !BlockUtils.isSensitive(this.protocol, material)) {
                        int slots = this.protocol.getOpenInventoryCountSlots(PlayerUtils.playerInventorySlots + 1);
                        boolean quick = slots <= PlayerUtils.playerInventorySlots;
                        boolean shift = click.isShiftClick();
                        boolean hotkey = click.isKeyboardClick();

                        // EXTENSION: Drag-Detection logic variables
                        int currentSlot = event.getRawSlot();
                        boolean slotChanged = (currentSlot != this.lastSlot);
                        this.lastSlot = currentSlot;

                        ItemStack cursor = event.getCursor();
                        boolean hasCursorItem = cursor != null && cursor.getType() != Material.AIR;

                        String type = BlockUtils.materialToString(material);

                        int newHash = type.hashCode();
                        int oldHash = this.oldHash;
                        this.oldHash = newHash;

                        long timePassed = System.currentTimeMillis() - time;
                        time = System.currentTimeMillis();

                        long safeTime = Math.max(timePassed, 1L);

                        // Detection 1: Auto-Armor / Hotkey Macros
                        if (hotkey && safeTime < 50L) {
                            if (shiftBuffer.count(1.5, 10) >= 5) {
                                this.shift.call(() -> this.shift.cancel("auto-armor/hotkey, ms: " + timePassed + ", item: " + type));
                            }
                        }
                        // Detection 2: Shift-Click Macros
                        else if (shift
                                && !quick
                                && !BlockUtils.hasMaterial(this.protocol.getOpenInventoryCursor())) {

                            if (safeTime < 60L) {
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

                                    double amplitude = Math.min(100.0 / (double) safeTime, 3.0);

                                    // EXTENSION: Catch Vanilla Click-Dragging and safely dampen the amplitude
                                    // Bypasses the false positive while ensuring Auto-Clickers on the same slot still flag.
                                    if (slotChanged && hasCursorItem && (click == ClickType.RIGHT || click == ClickType.LEFT)) {
                                        amplitude *= 0.15;
                                    }

                                    // Lambda expression finality requirement
                                    final double finalAmplitude = amplitude;

                                    this.slow.call(() -> {
                                        if (slowBuffer.count(finalAmplitude * 0.5, 20) >= 12) {
                                            this.slow.cancel("slow, ms: " + timePassed + ", item: " + type);
                                        }
                                    });
                                    if (timePassed < 100L) {
                                        this.medium.call(() -> {
                                            if (mediumBuffer.count(finalAmplitude * 0.75, 15) >= 8) {
                                                this.medium.cancel("medium, ms: " + timePassed + ", item: " + type);
                                            }
                                        });
                                        this.fast.call(() -> {
                                            if (timePassed < 50L && fastBuffer.count(finalAmplitude, 10) >= 6) {
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