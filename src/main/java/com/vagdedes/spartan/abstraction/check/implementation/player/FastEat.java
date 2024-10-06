package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;

public class FastEat extends CheckExecutor {

    private final DetectionExecutor interact, eat;
    private static final Material cake = MaterialUtils.get("cake");
    private static final long
            delay = 1_000L,
            driedKelp = 550L;

    private long delayTime, eatTime;

    public FastEat(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.interact = new ImplementedDetectionExecutor(this, "interact", true);
        this.eat = new ImplementedDetectionExecutor(this, "eat", true);
    }

    private long getDelay() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            PlayerInventory inventory = player.getInstance().getInventory();

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
        long time = System.currentTimeMillis() - delayTime;
        this.delayTime = System.currentTimeMillis();
        long delay = getDelay();

        if (delay > 0L && time <= delay) {
            this.eat.cancel("type: eat, ms: " + time);
            bool = true;
        } else {
            bool = false;
        }
        return bool;
    }

    private boolean checkEating() {
        long time = System.currentTimeMillis() - eatTime,
                delay = getDelay();

        if (delay > 0L && time <= delay) {
            this.interact.cancel("type: interact, ms: " + time + ", delay: " + delay);
            return true;
        }
        return false;
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof FoodLevelChangeEvent) {
            int lvl = ((FoodLevelChangeEvent) object).getFoodLevel();

            if (lvl > player.getInstance().getFoodLevel()
                    && (checkDelay() || checkEating())) {
                Player p = player.getInstance();
                p.setFoodLevel(p.getFoodLevel() - (lvl - p.getFoodLevel()));
            }
        } else if (!player.isBedrockPlayer()) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            switch (e.getAction()) {
                case RIGHT_CLICK_BLOCK:
                    if (e.getClickedBlock().getType() == cake) {
                        player.getExecutor(hackType).addDisableCause(hackType.toString(), null, 10);
                    }
                    break;
                case RIGHT_CLICK_AIR:
                    if (player.getItemInHand().getType().isEdible()) {
                        this.eatTime = System.currentTimeMillis();
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
