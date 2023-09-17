package me.vagdedes.spartan.checks.inventory;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ImpossibleInventory extends CheckExecutor {

    public static final HackType check = HackType.ImpossibleInventory;
    private static final Material
            nether_portal = MaterialUtils.get("nether_portal"),
            totem_of_undying = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11) ? Material.getMaterial("TOTEM_OF_UNDYING") : null; // Do not change, material changes mid-version
    private static final int
            jumping = 8,
            requiredJumpingMotions = AlgebraUtils.integerRound(MoveUtils.jumpingMotions / 2.0);
    public static final byte DEATH = 0;

    private static final double[] belowBlocks = new double[]{
            1, -1, 1
    };

    public ImpossibleInventory(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean handle(Object object) {
        if (object.equals(DEATH)) {
            if (canDo()) {
                int ticks = player.getCooldowns(check).get("auto-totem");

                if (ticks > 0) {
                    cancel("type: auto-totem, remaining-ticks: " + ticks);
                }
            }
        } else if (canDo()) {
            InventoryClickEvent e = (InventoryClickEvent) object;
            ItemStack item = e.getCurrentItem();
            runClosedInventory(e.getClick());
            runPortalInventory(item);
            trackAutoTotem(player, item);
        }
        return false;
    }

    @Override
    public boolean run() {
        if (canDo()
                && !player.isSwimming()
                && !player.getHandlers().has(Handlers.HandlerType.Vehicle)
                && PlayerData.isUsingTheCursor(player)) {
            int cases = getActionCase();

            if (cases != 0
                    && (cases != jumping
                    || player.getBuffer(check).start("cursor-usage", 20) >= 3)) {
                cancel("type: cursor-usage, case: " + cases, player.getEventFromLocation(), 0, false);
            }
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(check, true);
    }

    private void trackAutoTotem(SpartanPlayer p, ItemStack itemStack) {
        if (totem_of_undying != null && itemStack.getType() == totem_of_undying) {
            p.getCooldowns(check).add("auto-totem", 3);
        }
    }

    private void runClosedInventory(ClickType click) {
        int cases = getActionCase();

        if (cases != 0 && !click.isCreativeAction() && !click.isKeyboardClick() && !player.isSwimming()
                && !player.getHandlers().has(Handlers.HandlerType.Vehicle) && PlayerData.isUsingAnInventory(player, 0)
                && !PatternUtils.isBlockPattern(belowBlocks, player.getLocation(), true, BlockUtils.ice)
                && (cases != jumping || player.getBuffer(check).start("closed-inventory", 20) >= requiredJumpingMotions)) {
            cancel("type: closed-inventory, case: " + cases);
        }
    }

    private void runPortalInventory(ItemStack item) {
        if (item.getType() != Material.AIR) {
            SpartanLocation loc = player.getLocation();

            if (loc.getBlock().getType() == nether_portal || loc.clone().add(0, 1, 0).getBlock().getType() == nether_portal) {
                cancel("type: portal-inventory");
            }
        }
    }

    private int getActionCase() {
        if (player.wasFlying()) {
            return 0;
        }
        Check check = ImpossibleInventory.check.getCheck();
        return !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && check.getBooleanOption("check_sneaking", true) && player.isSneaking() ? 1 :
                !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && check.getBooleanOption("check_sprinting", true) && player.isSprinting() ? 2 :
                        check.getBooleanOption("check_sleeping", true) && player.isSleeping() ? 3 :
                                check.getBooleanOption("check_walking", true) && player.isWalking() ? 4 :
                                        check.getBooleanOption("check_dead", true) && player.isDead() ? 5 :
                                                check.getBooleanOption("check_sprint_jumping", true) && player.isSprintJumping() ? 6 :
                                                        check.getBooleanOption("check_walk_jumping", true) && player.isWalkJumping() ? 7 :
                                                                check.getBooleanOption("check_jumping", true) && player.isJumping(player.getNmsVerticalDistance()) ? jumping : 0;
    }
}
