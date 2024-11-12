package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.def.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
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

    public FastEat(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.interact = new ImplementedProbabilityDetection(this, "interact", true);
        this.eat = new ImplementedProbabilityDetection(this, "eat", true);
    }

    private long getDelay() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            PlayerInventory inventory = this.protocol().bukkit.getInventory();

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
            ItemStack itemStack = this.protocol().spartan.getItemInHand();

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
            this.eat.cancel(
                    "type: eat, ms: " + time,
                    delay / (double) time
            );
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
            this.interact.cancel(
                    "type: interact, ms: " + time + ", delay: " + delay,
                    delay / (double) time
            );
            return true;
        }
        return false;
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof FoodLevelChangeEvent) {
            int lvl = ((FoodLevelChangeEvent) object).getFoodLevel();

            if (lvl > this.protocol().bukkit.getFoodLevel()
                    && (checkDelay() || checkEating())) {
                Player p = this.protocol().bukkit;
                p.setFoodLevel(p.getFoodLevel() - (lvl - p.getFoodLevel()));
            }
        } else if (!this.protocol().spartan.isBedrockPlayer()) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            switch (e.getAction()) {
                case RIGHT_CLICK_BLOCK:
                    if (e.getClickedBlock().getType() == cake) {
                        this.protocol().spartan.getExecutor(hackType).addDisableCause(null, null, 10);
                    }
                    break;
                case RIGHT_CLICK_AIR:
                    if (this.protocol().spartan.getItemInHand().getType().isEdible()) {
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
        return this.protocol().spartan.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && !this.protocol().spartan.hasPotionEffect(PotionEffectType.SATURATION, 0)
                && Attributes.getAmount(this.protocol(), Attributes.GENERIC_MAX_ABSORPTION) == 0.0;
    }
}
