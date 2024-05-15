package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;

public class FastEat extends CheckExecutor {

    private static final Material cake = MaterialUtils.get("cake");

    private static final long
            delay = 1_000L,
            driedKelp = 550L;

    public FastEat(SpartanPlayer player) {
        super(HackType.FastEat, player);
    }

    private long getDelay() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            PlayerInventory inventory = player.getInventory();

            if (inventory != null) {
                for (ItemStack itemStack : new ItemStack[]{inventory.getItemInHand(), inventory.getItemInOffHand()}) {
                    if (itemStack != null) {
                        Material type = itemStack.getType();

                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && type == Material.DRIED_KELP) {
                            return driedKelp;
                        }
                        if (type.isEdible()) {
                            return delay;
                        }
                    }
                }
            }
        } else {
            ItemStack itemStack = player.getItemInHand();

            if (itemStack != null) {
                Material type = itemStack.getType();

                if (type.isEdible()) {
                    return delay;
                }
            }
        }
        return 0L;
    }

    private boolean checkDelay() {
        boolean bool;
        Timer timer = player.getTimer(hackType);
        long time = timer.get("time"),
                delay = getDelay();

        if (delay > 0L && time <= delay) {
            cancel("type: eat, ms: " + time);
            bool = true;
        } else {
            bool = false;
        }
        timer.set("time");
        return bool;
    }

    private boolean checkEating() {
        long time = player.getTimer(hackType).get("food"),
                delay = getDelay();

        if (delay > 0L && time <= delay) {
            cancel("type: interact, ms: " + time + ", delay: " + delay);
            return true;
        }
        return false;
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof FoodLevelChangeEvent) {
            int lvl = ((FoodLevelChangeEvent) object).getFoodLevel();

            if (lvl > player.getFoodLevel()
                    && (checkDelay() || checkEating())) {
                player.setFoodLevel(player.getFoodLevel() - (lvl - player.getFoodLevel()));
            }
        } else if (!player.bedrockPlayer) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            switch (e.getAction()) {
                case RIGHT_CLICK_BLOCK:
                    if (e.getClickedBlock().getType() == cake) {
                        player.getViolations(hackType).addDisableCause(hackType.toString(), null, 10);
                    }
                    break;
                case RIGHT_CLICK_AIR:
                    if (player.getItemInHand().getType().isEdible()) {
                        player.getTimer(hackType).set("food");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected boolean canRun() {
        return player.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && !player.hasPotionEffect(PotionEffectType.SATURATION, 0);
    }
}
