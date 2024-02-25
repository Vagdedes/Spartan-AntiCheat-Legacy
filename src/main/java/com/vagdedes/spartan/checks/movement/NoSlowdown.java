package com.vagdedes.spartan.checks.movement;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.handlers.stability.DetectionLocation;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.data.Handlers;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class NoSlowdown extends CheckExecutor {

    public static final HackType check = HackType.NoSlowdown;
    private static final Material cakeMaterial = MaterialUtils.get("cake");
    public static final byte PACKETS = 0;

    public NoSlowdown(SpartanPlayer player) {
        super(check, player);
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        if (hackType.getCheck().getBooleanOption("check_item_usage", true)
                && !player.getProfile().getCombat().isActivelyFighting(null, true, true, false)) {
            return runUse("item-use", player.isUsingItem(), 6);
        } else {
            return false;
        }
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerInteractEvent) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                    && canDo()
                    && hackType.getCheck().getBooleanOption("check_food_eating", true)
                    && e.getClickedBlock().getType() == cakeMaterial) {
                player.getCooldowns(hackType).add("food-eating", 10);
            }
        } else if (object instanceof EntityShootBowEvent) {
            Check check = hackType.getCheck();

            if (check.getBooleanOption("check_bow_shots", true)
                    && !PlayerData.hasJumpEffect(player)
                    && getMovementCase(player) != 0) {
                return canDo() && !PlayerData.hasSpeedEffect(player, player.getLocation());
            }
        } else if (object instanceof FoodLevelChangeEvent) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)
                    || !canDo() || !hackType.getCheck().getBooleanOption("check_food_eating", true)
                    || player.getWorld().getDifficulty() == Difficulty.PEACEFUL
                    || player.hasPotionEffect(PotionEffectType.SATURATION)
                    || !player.getCooldowns(hackType).canDo("food-eating")
                    || ((FoodLevelChangeEvent) object).getFoodLevel() <= player.getFoodLevel()) { // Item
                return false;
            }
            GameMode gameMode = player.getGameMode();

            if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) {
                return false;
            }
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                ItemStack item = player.getItemInHand();

                if (item != null && item.getType() == Material.DRIED_KELP) {
                    return false;
                }
            }
            int cases = getMovementCase(player);

            if (cases != 0) {
                cancel("type: food-eating, case: " + cases, DetectionLocation.get(player, true), 10, true);
            }
        } else if (object instanceof PlayerItemHeldEvent) {
            player.getCooldowns(hackType).add("held", 3);
        } else if (object.equals(PACKETS)) {
            runUse("packets", true, 1);
        } else if (hackType.getCheck().getBooleanOption("check_potion", true)
                && player.getItemInHand().getType() == Material.POTION) {
            runUse("potion", true, 1);
        }
        return false;
    }

    private boolean runUse(String reason, boolean itemUsage, int limit) {
        Double horizontal = player.getNmsHorizontalDistance(),
                oldHorizontal = player.getPreviousNmsHorizontalDistance(),
                vertical = player.getNmsVerticalDistance();

        if (horizontal == null
                || oldHorizontal == null
                || vertical == null) {
            return false;
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)
                && itemUsage
                && canDo()
                && player.getCooldowns(hackType).canDo("held")
                && player.getFoodLevel() < 20
                && horizontal >= 0.16

                && (oldHorizontal == 0.0
                || horizontal >= oldHorizontal
                || player.isOnGroundCustom()
                || !player.hasShield()
                && !PlayerData.holdsFood(player)
                && player.getItemInHand().getType() != Material.POTION)) {
            int cases = getMovementCase(player);

            if (cases != 0) {
                Material[] materials = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ?
                        new Material[]{player.getItemInHand().getType(), player.getInventory().itemInOffHand.getType()} :
                        new Material[]{player.getItemInHand().getType()};
                StringBuilder items = new StringBuilder();
                boolean allAir = true;

                for (Material material : materials) {
                    if (material == Material.FISHING_ROD) {
                        return false;
                    } else {
                        items.append(items.length() == 0 ? "" : ", ").append(BlockUtils.materialToString(material));

                        if (!BlockUtils.areAir(material)) {
                            allAir = false;
                        }
                    }
                }

                if (!allAir) {
                    Buffer buffer = player.getBuffer(hackType);
                    String key = "use=" + reason;

                    if (limit == 1 || buffer.start(key, 10) == limit) {
                        buffer.remove(key);
                        String verbose = "type: " + reason
                                + ", holding: " + items
                                + ", case: " + cases
                                + ", last-sprint: " + player.getLastOffSprint()
                                + ", horizontal: " + horizontal
                                + ", vertical: " + vertical;
                        cancel(verbose, DetectionLocation.get(player, true), 10, true);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true) && !player.isSwimming() && !player.wasInLiquids()
                && !player.getHandlers().has(Handlers.HandlerType.Vehicle) && !player.getHandlers().has(Handlers.HandlerType.Explosion)
                && !player.getHandlers().has(Handlers.HandlerType.ElytraUse);
    }

    private int getMovementCase(SpartanPlayer p) {
        return p.isSprinting() ? 1 :
                p.isSprintJumping() ? 3 :
                        p.isWalkJumping() ? 4 : 0;
    }
}
