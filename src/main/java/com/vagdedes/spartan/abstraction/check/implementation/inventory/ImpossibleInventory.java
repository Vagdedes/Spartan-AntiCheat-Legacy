package com.vagdedes.spartan.abstraction.check.implementation.inventory;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
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

    public ImpossibleInventory(Enums.HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.autoTotemDetection = new ImplementedProbabilityDetection(this, "auto_totem", true);
        this.closedInventoryDetection = new ImplementedProbabilityDetection(this, "closed_inventory", true);
        this.portalInventoryDetection = new ImplementedProbabilityDetection(this, "portal_inventory", true);
        this.cursorUsageDetection = new ImplementedProbabilityDetection(this, "cursor_usage", true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object == null) {
            long ticks = this.autoTotem - System.currentTimeMillis();

            if (ticks > 0L) {
                this.autoTotemDetection.cancel("type: auto-totem, remaining-ticks: " + ticks);
            }
        } else {
            InventoryClickEvent e = (InventoryClickEvent) object;
            ItemStack item = e.getCurrentItem();
            runClosedInventory(e.getClick());
            runPortalInventory(item);
            trackAutoTotem(item);
        }
    }

    @Override
    protected void runInternal(boolean cancelled) {
        if (!this.protocol.spartan.movement.isSwimming() && this.protocol.spartan.getVehicle() == null) {
            ItemStack cursor = this.protocol.bukkit().getOpenInventory().getCursor();

            if (BlockUtils.hasMaterial(cursor)) {
                int cases = getActionCase();

                if (cases != 0) {
                    this.cursorUsageDetection.cancel(
                            "type: cursor-usage, case: " + cases,
                            this.protocol.getFromLocation(),
                            0,
                            false
                    );
                }
            }
        }
    }

    private void trackAutoTotem(ItemStack itemStack) {
        if (totem_of_undying != null
                && itemStack != null
                && itemStack.getType() == totem_of_undying) {
            this.autoTotem = System.currentTimeMillis() + (3L * TPS.tickTime);
        }
    }

    private void runClosedInventory(ClickType click) {
        int cases = getActionCase();

        if (cases != 0
                && !click.isCreativeAction()
                && !click.isKeyboardClick()
                && !this.protocol.spartan.movement.isSwimming()
                && this.protocol.spartan.getVehicle() == null
                && this.protocol.bukkit().getOpenInventory().countSlots() > PlayerUtils.playerInventorySlots
                && !BlockPatternUtils.isBlockPattern(
                belowBlocks,
                this.protocol.getLocationOrVehicle(),
                true,
                BlockUtils.ice, BlockUtils.blue_ice)) {
            this.closedInventoryDetection.cancel("type: closed-inventory, case: " + cases);
        }
    }

    private void runPortalInventory(ItemStack item) {
        if (BlockUtils.hasMaterial(item)) {
            SpartanLocation loc = new SpartanLocation(this.protocol.getLocation());

            if (loc.getBlock().getType() == nether_portal
                    || loc.clone().add(0, 1, 0).getBlock().getType() == nether_portal) {
                this.portalInventoryDetection.cancel("type: portal-inventory");
            }
        }
    }

    private int getActionCase() {
        if (this.protocol.spartan.movement.isFlying()
                || this.protocol.spartan.movement.isLowEyeHeight()) {
            return 0;
        } else {
            Check check = hackType.getCheck();
            return check.getBooleanOption("check_sneaking", true)
                    && this.protocol.isSneaking() ? 1
                    : !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                    && check.getBooleanOption("check_sprinting", true)
                    && this.protocol.isSprinting() ? 2
                    : check.getBooleanOption("check_sleeping", true)
                    && this.protocol.bukkit().isSleeping() ? 3
                    : check.getBooleanOption("check_dead", true)
                    && this.protocol.spartan.isDead() ? 5
                    : check.getBooleanOption("check_sprint_jumping", true)
                    && this.protocol.isSprinting()
                    && this.protocol.spartan.movement.isJumping(this.protocol.spartan.movement.getValueOrDefault(this.protocol.spartan.movement.getEventVertical(), 0.0)) ? 6
                    : check.getBooleanOption("check_walk_jumping", true)
                    && !this.protocol.isSprinting()
                    && this.protocol.spartan.movement.isJumping(this.protocol.spartan.movement.getValueOrDefault(this.protocol.spartan.movement.getEventVertical(), 0.0)) ? 7
                    : check.getBooleanOption("check_jumping", true)
                    && this.protocol.spartan.movement.isJumping(this.protocol.spartan.movement.getValueOrDefault(this.protocol.spartan.movement.getEventVertical(), 0.0)) ? 8
                    : 0;
        }
    }

}
