package ai.idealistic.spartan.abstraction.check.implementation.misc;

import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.abstraction.world.ServerLocation;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.utils.minecraft.entity.PlayerUtils;
import ai.idealistic.spartan.utils.minecraft.inventory.MaterialUtils;
import ai.idealistic.spartan.utils.minecraft.world.BlockPatternUtils;
import ai.idealistic.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class ImpossibleInventory extends CheckRunner {

    private final CheckDetection
            autoTotemDetection,
            closedInventoryDetection,
            portalInventoryDetection,
            cursorUsageDetection;
    private static final Material
            nether_portal = MaterialUtils.get("nether_portal"),
            totem_of_undying = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)
                    ? Material.getMaterial("TOTEM_OF_UNDYING")
                    : null;

    private static final double[] belowBlocks = new double[]{
            1, -1, 1
    };

    // EXTENSION: Replaced broken Death-Tracking with Damage-Reaction Tracking
    private long lastDamageTime = 0L;
    private double autoTotemVl = 0;

    // EXTENSION: Added buffers to absorb legit momentum desyncs
    private double cursorVl = 0;
    private double invMoveVl = 0;
    private double portalVl = 0;

    public ImpossibleInventory(CheckEnums.HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.autoTotemDetection = new ImplementedDetection(this, null, null, "auto_totem", true);
        this.closedInventoryDetection = new ImplementedDetection(this, null, null, "closed_inventory", true);
        this.portalInventoryDetection = new ImplementedDetection(this, null, null, "portal_inventory", true);
        this.cursorUsageDetection = new ImplementedDetection(this, null, null, "cursor_usage", true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        // EXTENSION: Hook damage to measure inhuman reaction times for AutoTotem
        if (object instanceof EntityDamageEvent) {
            this.lastDamageTime = System.currentTimeMillis();
        } else if (object instanceof InventoryClickEvent) {
            InventoryClickEvent e = (InventoryClickEvent) object;
            ItemStack item = e.getCurrentItem();
            ItemStack cursor = e.getCursor();

            runClosedInventory(e.getClick());
            runPortalInventory(item);
            trackAutoTotem(item, cursor);

            if (this.prevent()) {
                e.setCancelled(true);
            }
        } else if (object instanceof PlayerMoveEvent) {
            PlayerMoveEvent event = (PlayerMoveEvent) object;

            this.cursorUsageDetection.call(() -> {
                if (!this.protocol.isSwimming()
                        && this.protocol.getVehicle() == null
                        && BlockUtils.hasMaterial(this.protocol.getOpenInventoryCursor())) {

                    int cases = getActionCase(event);

                    if (cases != 0) {
                        if (++cursorVl > 4) { // Buffer to absorb mid-air inventory clutches
                            this.cursorUsageDetection.cancel(
                                    "cursor-usage, case: " + cases,
                                    this.protocol.getFromLocation(),
                                    0,
                                    false
                            );
                        }
                    } else {
                        cursorVl = Math.max(0, cursorVl - 0.5);
                    }
                }
            });
        }
    }

    private void trackAutoTotem(ItemStack itemStack, ItemStack cursor) {
        this.autoTotemDetection.call(() -> {
            if (totem_of_undying != null) {
                boolean interactingWithTotem = (itemStack != null && itemStack.getType() == totem_of_undying) ||
                        (cursor != null && cursor.getType() == totem_of_undying);

                if (interactingWithTotem) {
                    long reactionTime = System.currentTimeMillis() - lastDamageTime;

                    // A cheat client reacts instantly to damage/totem pops (< 50ms)
                    // Human reaction time to a health drop is > 150ms minimum.
                    if (reactionTime < 50L) {
                        if (++autoTotemVl > 1) {
                            this.autoTotemDetection.cancel("auto-totem (reaction: " + reactionTime + "ms)");
                            autoTotemVl = 0;
                        }
                    } else {
                        autoTotemVl = Math.max(0, autoTotemVl - 0.1);
                    }
                }
            }
        });
    }

    private void runClosedInventory(ClickType click) {
        this.closedInventoryDetection.call(() -> {
            // Passing null since we don't have move event deltas here, relies on boolean states
            int cases = getActionCase(null);

            if (cases != 0
                    && !click.isCreativeAction()
                    && !click.isKeyboardClick()
                    && !this.protocol.isSwimming()
                    && this.protocol.getVehicle() == null
                    && this.protocol.getOpenInventoryCountSlots(0) > PlayerUtils.playerInventorySlots
                    && !BlockPatternUtils.isBlockPattern(
                    belowBlocks,
                    this.protocol.getLocationOrVehicle(),
                    true,
                    BlockUtils.ice, BlockUtils.blue_ice)) {

                if (++invMoveVl > 6) { // Buffer to prevent push/knockback false positives
                    this.closedInventoryDetection.cancel("closed-inventory, case: " + cases);
                }
            } else {
                invMoveVl = Math.max(0, invMoveVl - 0.25);
            }
        });
    }

    private void runPortalInventory(ItemStack item) {
        this.portalInventoryDetection.call(() -> {
            if (this.protocol.getGameMode() == GameMode.CREATIVE) return; // Portals allow GUI in Creative

            if (BlockUtils.hasMaterial(item)) {
                ServerLocation loc = new ServerLocation(this.protocol.getLocation());

                if (loc.getBlock(this.protocol).getType() == nether_portal
                        || loc.clone().add(0, 1, 0).getBlock(this.protocol).getType() == nether_portal) {

                    if (++portalVl > 3) { // Buffer to absorb edge-case desyncs
                        this.portalInventoryDetection.cancel("portal-inventory");
                    }
                } else {
                    portalVl = Math.max(0, portalVl - 0.5);
                }
            }
        });
    }

    private int getActionCase(PlayerMoveEvent event) {
        if (this.protocol.isFlying() || this.protocol.isLowEyeHeight()) {
            return 0;
        } else {
            Check check = hackType.getCheck();

            // EXTENSION: Sealed the InvMove Walking Bypass
            if (event != null && check.getBooleanOption("check_walking", true)) {
                double deltaX = event.getTo().getX() - event.getFrom().getX();
                double deltaZ = event.getTo().getZ() - event.getFrom().getZ();
                double horizontalSpeedSq = (deltaX * deltaX) + (deltaZ * deltaZ);

                // If moving faster than natural drift/slip while GUI is open, it's InvMove
                if (horizontalSpeedSq > 0.01 && this.protocol.isOnGround()) {
                    return 9; // Custom case for horizontal walking
                }
            }

            return check.getBooleanOption("check_sneaking", true)
                    && this.protocol.isSneaking() ? 1
                    : !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                    && check.getBooleanOption("check_sprinting", true)
                    && this.protocol.isSprinting() ? 2
                      : check.getBooleanOption("check_sleeping", true)
                    && this.protocol.bukkit().isSleeping() ? 3
                        : check.getBooleanOption("check_dead", true)
                    && this.protocol.bukkit().isDead() ? 5
                          : check.getBooleanOption("check_sprint_jumping", true)
                    && this.protocol.isSprinting()
                    && this.protocol.isJumping(this.getVertical()) ? 6
                            : check.getBooleanOption("check_walk_jumping", true)
                    && !this.protocol.isSprinting()
                    && this.protocol.isJumping(this.getVertical()) ? 7
                              : check.getBooleanOption("check_jumping", true)
                    && this.protocol.justJumped(this.getVertical()) ? 8
                                : 0;
        }
    }

    private double getVertical() {
        return this.protocol.getLocation().getY() - this.protocol.getFromLocation().getY();
    }
}