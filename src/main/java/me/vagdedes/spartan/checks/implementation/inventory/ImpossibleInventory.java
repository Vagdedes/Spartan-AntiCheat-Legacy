package me.vagdedes.spartan.checks.implementation.inventory;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ImpossibleInventory {

    public static final HackType check = HackType.ImpossibleInventory;
    private static final Material
            nether_portal = MaterialUtils.get("nether_portal"),
            totem_of_undying = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11) ? Material.getMaterial("TOTEM_OF_UNDYING") : null; // Do not change, material changes mid-version
    private static final int
            jumping = 8,
            requiredJumpingMotions = AlgebraUtils.integerRound(MoveUtils.jumpingMotions / 2.0);

    private static double[] belowBlocks = new double[]{
            1, -1, 1
    };

    public static void run(SpartanPlayer p, ItemStack item, ClickType click) {
        if (p.canRunCheck(check, true)) {
            runClosedInventory(p, click);
            runPortalInventory(p, item);
            trackAutoTotem(p, item);
        }
    }

    // Separator

    private static void trackAutoTotem(SpartanPlayer p, ItemStack itemStack) {
        if (totem_of_undying != null && itemStack.getType() == totem_of_undying) {
            p.getCooldowns(check).add("auto-totem", 3);
        }
    }

    public static void checkAutoTotem(SpartanPlayer p) {
        if (p.canRunCheck(check, true)) {
            int ticks = p.getCooldowns(check).get("auto-totem");

            if (ticks > 0) {
                new HackPrevention(p, check, "type: auto-totem, remaining-ticks: " + ticks);
            }
        }
    }

    // Separator

    private static void runClosedInventory(SpartanPlayer p, ClickType click) {
        int cases = getActionCase(p, Double.MAX_VALUE);

        if (cases != 0 && !click.isCreativeAction() && !click.isKeyboardClick() && !p.isSwimming()
                && !p.getHandlers().has(Handlers.HandlerType.Vehicle) && PlayerData.isUsingAnInventory(p, 0)
                && !PatternUtils.isBlockPattern(belowBlocks, p.getLocation(), true, BlockUtils.ice)
                && (cases != jumping || p.getBuffer(check).start("closed-inventory", 20) >= requiredJumpingMotions)) {
            new HackPrevention(p, check, "type: closed-inventory, case: " + cases);
        }
    }

    private static void runPortalInventory(SpartanPlayer p, ItemStack item) {
        if (item.getType() != Material.AIR) {
            SpartanLocation loc = p.getLocation();

            if (loc.getBlock().getType() == nether_portal || loc.clone().add(0, 1, 0).getBlock().getType() == nether_portal) {
                new HackPrevention(p, check, "type: portal-inventory");
            }
        }
    }

    // Separator

    public static void runCursorUsage(SpartanPlayer p, SpartanLocation from, double ver) {
        if (p.canRunCheck(check, true) && !p.isSwimming()
                && !p.getHandlers().has(Handlers.HandlerType.Vehicle) && PlayerData.isUsingTheCursor(p)) {
            int cases = getActionCase(p, ver);

            if (cases != 0 && (cases != jumping || p.getBuffer(check).start("cursor-usage", 20) >= 3)) {
                new HackPrevention(p, check, "type: cursor-usage, case: " + cases, from, 0, false);
            }
        }
    }

    // Separator

    private static int getActionCase(SpartanPlayer p, double ver) {
        if (p.wasFlying()) {
            return 0;
        }
        Check check = ImpossibleInventory.check.getCheck();
        return !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && check.getBooleanOption("check_sneaking", true) && p.isSneaking() ? 1 :
                !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && check.getBooleanOption("check_sprinting", true) && p.isSprinting() ? 2 :
                        check.getBooleanOption("check_sleeping", true) && p.isSleeping() ? 3 :
                                check.getBooleanOption("check_walking", true) && p.isWalking() ? 4 :
                                        check.getBooleanOption("check_dead", true) && p.isDead() ? 5 :
                                                check.getBooleanOption("check_sprint_jumping", true) && p.isSprintJumping() ? 6 :
                                                        check.getBooleanOption("check_walk_jumping", true) && p.isWalkJumping() ? 7 :
                                                                check.getBooleanOption("check_jumping", true) && p.isJumping(ver != Double.MAX_VALUE ? ver : p.getNmsVerticalDistance()) ? jumping : 0;
    }
}
