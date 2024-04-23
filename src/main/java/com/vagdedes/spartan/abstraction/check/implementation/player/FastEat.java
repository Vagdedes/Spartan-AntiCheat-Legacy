package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Cooldowns;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.server.MaterialUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class FastEat extends CheckExecutor {

    private static final HackType check = HackType.FastEat;
    private static final Material cake = MaterialUtils.get("cake");

    private static final long
            delay = 1000L,
            driedKelp = 550L;
    private static final int ticks = 25;

    public FastEat(SpartanPlayer player) {
        super(check, player);
    }

    private long getDelay() {
        for (ItemStack itemStack : new ItemStack[]{player.getItemInHand(), player.getInventory().itemInOffHand}) {
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

    private void goldenAppleEffects(SpartanPlayer p) {
        ItemStack item = p.getItemInHand();

        if (item != null
                && item.getType() == Material.GOLDEN_APPLE) {
            if (!p.hasPotionEffect(PotionEffectType.REGENERATION, 0)) {
                p.getCooldowns(hackType).add("regeneration", ticks);
            }
            if (!p.hasPotionEffect(PotionEffectType.ABSORPTION, 0)) {
                p.getCooldowns(hackType).add("absorption", ticks);
            }
            if (!p.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 0)) {
                p.getCooldowns(hackType).add("resistance", ticks);
            }
        }
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        if (object instanceof FoodLevelChangeEvent) {
            if (canDo()) {
                int lvl = ((FoodLevelChangeEvent) object).getFoodLevel();

                if (lvl > player.getFoodLevel()
                        && (checkDelay() || checkEating())) {
                    Cooldowns cooldowns = player.getCooldowns(hackType);
                    player.setFoodLevel(player.getFoodLevel() - (lvl - player.getFoodLevel()), true);

                    if (!cooldowns.canDo("regeneration")) {
                        player.removePotionEffect(PotionEffectType.REGENERATION);
                    }
                    if (!cooldowns.canDo("absorption")) {
                        player.removePotionEffect(PotionEffectType.ABSORPTION);
                    }
                    if (!cooldowns.canDo("resistance")) {
                        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    }
                }
            }
        } else if (!player.bedrockPlayer && canDo()) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            switch (e.getAction()) {
                case RIGHT_CLICK_BLOCK:
                    if (e.getClickedBlock().getType() == cake) {
                        player.getViolations(hackType).addDisableCause(hackType.toString(), null, 10);
                    }
                    break;
                case RIGHT_CLICK_AIR:
                    if (player.getItemInHand().getType().isEdible()) {
                        goldenAppleEffects(player);
                        player.getTimer(hackType).set("food");
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && player.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && !player.hasPotionEffect(PotionEffectType.SATURATION, 0);
    }
}
