package me.vagdedes.spartan.checks.movement;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Handlers;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class NoSlowdown {

    public static final HackType check = HackType.NoSlowdown;
    private static final Material cakeMaterial = MaterialUtils.get("cake");

    public static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from, double ver, double hor, double oHor) {
        if (check.getCheck().getBooleanOption("check_item_usage", true) && !PlayerData.isInActivePlayerCombat(p)) {
            runUse("item-use", p.isUsingItem(), 6, p, to, from, hor, oHor, ver);
        }
    }

    public static void runPotion(SpartanPlayer p) {
        ItemStack item;

        if (check.getCheck().getBooleanOption("check_potion", true) && !LagLeniencies.hasHighLatency(p, LagLeniencies.badPing) // Item
                && (item = p.getItemInHand()) != null && item.getType() == Material.POTION) {
            runUse("potion", true, 1, p, p.getLocation(),
                    MoveUtils.getCachedLocation(p), p.getNmsHorizontalDistance(), 0, p.getNmsVerticalDistance());
        }
    }

    // Food protection

    public static void runCake(SpartanPlayer p, SpartanBlock b, Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK && canDo(p)
                && check.getCheck().getBooleanOption("check_food_eating", true)
                && b.getType() == cakeMaterial) {
            p.getCooldowns(check).add("food-eating", 10);
        }
    }

    public static void runFood(SpartanPlayer p, int lvl) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)
                || !canDo(p) || !check.getCheck().getBooleanOption("check_food_eating", true)
                || p.getWorld().getDifficulty() == Difficulty.PEACEFUL
                || p.hasPotionEffect(PotionEffectType.SATURATION)
                || !p.getCooldowns(check).canDo("food-eating") || lvl <= p.getFoodLevel()
                || LagLeniencies.hasHighLatency(p, LagLeniencies.badPing)) { // Item
            return;
        }
        GameMode gameMode = p.getGameMode();

        if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) {
            return;
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            ItemStack item = p.getItemInHand();

            if (item != null && item.getType() == Material.DRIED_KELP) {
                return;
            }
        }
        int cases = getMovementCase(p, false);

        if (cases != 0) {
            new HackPrevention(p, check, "type: food-eating, case: " + cases, p.getLocation(), 10, true);
        }
    }

    // Bow protection

    public static boolean runBow(SpartanPlayer p) {
        Check check = NoSlowdown.check.getCheck();

        if (!check.isSilent(p.getWorld().getName(), p.getUniqueId())
                && check.getBooleanOption("check_bow_shots", true)
                && !PlayerData.hasJumpEffect(p)
                && getMovementCase(p, true) != 0) {
            SpartanLocation loc = p.getLocation();
            return canDo(p)
                    && !PlayerData.hasSpeedEffect(p, loc);
        }
        return false;
    }

    // Use protection

    public static void runUse(String reason, boolean itemUsage, int limit,
                              SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                              double horizontal, double oldHorizontal, double vertical) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                && itemUsage
                && canDo(p)
                && p.getCooldowns(check).canDo("held")
                && p.getFoodLevel() < 20
                && horizontal >= 0.16

                && (oldHorizontal == 0.0
                || horizontal >= oldHorizontal
                || p.isOnGroundCustom()
                || !p.hasShield()
                && !PlayerData.holdsFood(p)
                && p.getItemInHand().getType() != Material.POTION)) {
            int cases = getMovementCase(p, false);

            if (cases != 0) {
                Material[] materials = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ?
                        new Material[]{p.getItemInHand().getType(), p.getInventory().getItemInOffHand().getType()} :
                        new Material[]{p.getItemInHand().getType()};
                StringBuilder items = new StringBuilder();
                boolean allAir = true;

                for (Material material : materials) {
                    if (material == Material.FISHING_ROD) {
                        return;
                    } else {
                        items.append(items.length() == 0 ? "" : ", ").append(BlockUtils.materialToString(material));

                        if (!BlockUtils.areAir(material)) {
                            allAir = false;
                        }
                    }
                }

                if (!allAir) {
                    Buffer buffer = p.getBuffer(check);
                    String key = "use=" + reason;

                    if (limit == 1 || buffer.start(key, 10) == limit) {
                        buffer.remove(key);
                        String verbose = "type: " + reason
                                + ", holding: " + items
                                + ", case: " + cases
                                + ", last-sprint: " + p.getLastOffSprint()
                                + ", horizontal: " + horizontal
                                + ", vertical: " + vertical;
                        new HackPrevention(p, check, verbose, from, 10, true);
                    }
                }
            }
        }
    }

    public static void runItemChange(SpartanPlayer p) {
        p.getCooldowns(check).add("held", 3);
    }

    // Basic Utils

    private static boolean canDo(SpartanPlayer p) {
        return p.canRunCheck(check, true) && !p.isSwimming() && !p.wasInLiquids()
                && !p.getHandlers().has(Handlers.HandlerType.Vehicle) && !p.getHandlers().has(Handlers.HandlerType.Explosion)
                && !p.getHandlers().has(Handlers.HandlerType.ElytraUse);
    }

    private static int getMovementCase(SpartanPlayer p, boolean lenient) {
        return (lenient ? p.isCustomSprinting() : p.isSprinting()) ? 1 :
                p.isWalking() ? 2 :
                        p.isSprintJumping() ? 3 :
                                p.isWalkJumping() ? 4 : 0;
    }
}
