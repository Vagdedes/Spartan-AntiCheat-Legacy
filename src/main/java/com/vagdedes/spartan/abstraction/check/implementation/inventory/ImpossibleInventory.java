package com.vagdedes.spartan.abstraction.check.implementation.inventory;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.server.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.MaterialUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ImpossibleInventory extends CheckExecutor {

    private static final Material
            nether_portal = MaterialUtils.get("nether_portal"),
            totem_of_undying = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)
                    ? Material.getMaterial("TOTEM_OF_UNDYING")
                    : null; // Do not change, material changes mid-version

    private static final double[] belowBlocks = new double[]{
            1, -1, 1
    };

    private final Cooldowns cooldowns;

    public ImpossibleInventory(SpartanPlayer player) {
        super(Enums.HackType.ImpossibleInventory, player);
        cooldowns = new Cooldowns(player);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object == null) {
            int ticks = cooldowns.get("auto-totem");

            if (ticks > 0) {
                cancel("type: auto-totem, remaining-ticks: " + ticks);
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
        if (!player.movement.isSwimming()
                && player.getVehicle() == null) {
            ItemStack cursor = player.getOpenInventory().getCursor();

            if (BlockUtils.hasMaterial(cursor)) {
                int cases = getActionCase();

                if (cases != 0) {
                    cancel(
                            "type: cursor-usage, case: " + cases,
                            1.0,
                            player.movement.getEventFromLocation(),
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
            cooldowns.add("auto-totem", 3);
        }
    }

    private void runClosedInventory(ClickType click) {
        int cases = getActionCase();

        if (cases != 0
                && !click.isCreativeAction()
                && !click.isKeyboardClick()
                && !player.movement.isSwimming()
                && player.getVehicle() == null
                && player.getOpenInventory().countSlots() > PlayerUtils.playerInventorySlots
                && !BlockPatternUtils.isBlockPattern(belowBlocks, player.movement.getLocation(), true, BlockUtils.ice)) {
            cancel("type: closed-inventory, case: " + cases);
        }
    }

    private void runPortalInventory(ItemStack item) {
        if (BlockUtils.hasMaterial(item)) {
            SpartanLocation loc = player.movement.getLocation();

            if (loc.getBlock().material == nether_portal
                    || loc.clone().add(0, 1, 0).getBlock().material == nether_portal) {
                cancel("type: portal-inventory");
            }
        }
    }

    private int getActionCase() {
        if (player.movement.isFlying() || player.movement.isCrawling()) {
            return 0;
        } else {
            Check check = hackType.getCheck();
            return check.getBooleanOption("check_sneaking", true)
                    && player.movement.isSneaking() ? 1
                    : !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)
                    && check.getBooleanOption("check_sprinting", true)
                    && player.movement.isSprinting() ? 2
                    : check.getBooleanOption("check_sleeping", true)
                    && player.isSleeping() ? 3
                    : check.getBooleanOption("check_dead", true)
                    && player.isDead() ? 5
                    : check.getBooleanOption("check_sprint_jumping", true)
                    && player.movement.isSprintJumping(player.movement.getValueOrDefault(player.movement.getEventVertical(), 0.0)) ? 6
                    : check.getBooleanOption("check_walk_jumping", true)
                    && player.movement.isWalkJumping(player.movement.getValueOrDefault(player.movement.getEventVertical(), 0.0)) ? 7
                    : check.getBooleanOption("check_jumping", true)
                    && player.movement.isJumping(player.movement.getValueOrDefault(player.movement.getEventVertical(), 0.0)) ? 8
                    : 0;
        }
    }
}
