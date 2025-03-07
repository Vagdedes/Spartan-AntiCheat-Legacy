package com.vagdedes.spartan.abstraction.check.implementation.inventory;

import com.comphenix.protocol.PacketType;
import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedDetection;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.listeners.protocol.DeathListener;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.entity.PlayerDeathEvent;
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
                    : null; // Do not change, material changes mid-version

    private static final double[] belowBlocks = new double[]{
            1, -1, 1
    };

    private long autoTotem;

    public ImpossibleInventory(Enums.HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.autoTotemDetection = new ImplementedDetection(this, null, null, "auto_totem", true);
        this.closedInventoryDetection = new ImplementedDetection(this, null, null, "closed_inventory", true);
        this.portalInventoryDetection = new ImplementedDetection(this, null, null, "portal_inventory", true);
        this.cursorUsageDetection = new ImplementedDetection(this, null, null, "cursor_usage", true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerDeathEvent) {
            this.death();
        } else if (object instanceof InventoryClickEvent) {
            InventoryClickEvent e = (InventoryClickEvent) object;
            ItemStack item = e.getCurrentItem();
            runClosedInventory(e.getClick());
            runPortalInventory(item);
            trackAutoTotem(item);

            if (this.prevent()) {
                e.setCancelled(true);
            }
        } else if (object instanceof PlayerMoveEvent) {
            this.cursorUsageDetection.call(() -> {
                if (!this.protocol.bukkitExtra.isSwimming() && this.protocol.bukkitExtra.getVehicle() == null) {
                    ItemStack cursor = this.protocol.bukkit().getOpenInventory().getCursor();

                    if (BlockUtils.hasMaterial(cursor)) {
                        int cases = getActionCase();

                        if (cases != 0) {
                            this.cursorUsageDetection.cancel(
                                    "cursor-usage, case: " + cases,
                                    this.protocol.getFromLocation(),
                                    0,
                                    false
                            );
                        }
                    }
                }
            });
        } else if (PluginBase.packetsEnabled()
                && object instanceof PacketType) {
            for (PacketType type : DeathListener.packetTypes) {
                if (type.equals(object)) {
                    this.death();
                    break;
                }
            }
        }
    }

    private void trackAutoTotem(ItemStack itemStack) {
        this.autoTotemDetection.call(() -> {
            if (totem_of_undying != null
                    && itemStack != null
                    && itemStack.getType() == totem_of_undying) {
                this.autoTotem = System.currentTimeMillis() + (3L * TPS.tickTime);
            }
        });
    }

    private void death() {
        long ticks = this.autoTotem - System.currentTimeMillis();

        if (ticks > 0L) {
            this.autoTotemDetection.cancel("auto-totem, remaining-ticks: " + ticks);
        }
    }

    private void runClosedInventory(ClickType click) {
        this.closedInventoryDetection.call(() -> {
            int cases = getActionCase();

            if (cases != 0
                    && !click.isCreativeAction()
                    && !click.isKeyboardClick()
                    && !this.protocol.bukkitExtra.isSwimming()
                    && this.protocol.bukkitExtra.getVehicle() == null
                    && this.protocol.bukkit().getOpenInventory().countSlots() > PlayerUtils.playerInventorySlots
                    && !BlockPatternUtils.isBlockPattern(
                    belowBlocks,
                    this.protocol.getLocationOrVehicle(),
                    true,
                    BlockUtils.ice, BlockUtils.blue_ice)) {
                this.closedInventoryDetection.cancel("closed-inventory, case: " + cases);
            }
        });
    }

    private void runPortalInventory(ItemStack item) {
        this.portalInventoryDetection.call(() -> {
            if (BlockUtils.hasMaterial(item)) {
                SpartanLocation loc = new SpartanLocation(this.protocol.getLocation());

                if (loc.getBlock().getType() == nether_portal
                        || loc.clone().add(0, 1, 0).getBlock().getType() == nether_portal) {
                    this.portalInventoryDetection.cancel("portal-inventory");
                }
            }
        });
    }

    private int getActionCase() {
        if (this.protocol.bukkitExtra.isFlying()
                || this.protocol.isLowEyeHeight()) {
            return 0;
        } else {
            Check check = hackType.getCheck();
            return check.getBooleanOption("check_sneaking", true)
                    && this.protocol.bukkitExtra.isSneaking() ? 1
                    : !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                    && check.getBooleanOption("check_sprinting", true)
                    && this.protocol.bukkitExtra.isSprinting() ? 2
                    : check.getBooleanOption("check_sleeping", true)
                    && this.protocol.bukkit().isSleeping() ? 3
                    : check.getBooleanOption("check_dead", true)
                    && this.protocol.bukkit().isDead() ? 5
                    : check.getBooleanOption("check_sprint_jumping", true)
                    && this.protocol.bukkitExtra.isSprinting()
                    && this.protocol.isJumping(this.getVertical()) ? 6
                    : check.getBooleanOption("check_walk_jumping", true)
                    && !this.protocol.bukkitExtra.isSprinting()
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
