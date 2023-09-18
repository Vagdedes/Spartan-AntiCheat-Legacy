package me.vagdedes.spartan.checks.player;

import me.vagdedes.spartan.abstraction.CheckExecutor;
import me.vagdedes.spartan.functionality.important.MultiVersion;
import me.vagdedes.spartan.functionality.protections.LagLeniencies;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class FastEat extends CheckExecutor {

    private static final HackType check = HackType.FastEat;
    private static final Material cake = MaterialUtils.get("cake");

    private static final long delay = 1000L;
    private static final long driedKelp = 550L;
    private static final int ticks = 25;

    public FastEat(SpartanPlayer player) {
        super(check, player);
    }

    private long getDelay() {
        for (ItemStack itemStack : new ItemStack[]{player.getItemInHand(), player.getInventory().getItemInOffHand()}) {
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
            if (!p.hasPotionEffect(PotionEffectType.REGENERATION)) {
                p.getCooldowns(hackType).add("regeneration", ticks);
            }
            if (!p.hasPotionEffect(PotionEffectType.ABSORPTION)) {
                p.getCooldowns(hackType).add("absorption", ticks);
            }
            if (!p.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                p.getCooldowns(hackType).add("resistance", ticks);
            }
        }
    }

    @Override
    public boolean handle(Object object) {
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
        } else if (!player.isBedrockPlayer() && canDo()) {
            PlayerInteractEvent e = (PlayerInteractEvent) object;

            switch (e.getAction()) {
                case RIGHT_CLICK_BLOCK:
                    if (e.getClickedBlock().getType() == cake) {
                        hackType.getCheck().addDisabledUser(player.getUniqueId(), hackType.toString(), 10);
                    }
                    break;
                case RIGHT_CLICK_AIR:
                    if (PlayerData.holdsFood(player)) {
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
    public boolean run() {
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && player.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && !player.hasPotionEffect(PotionEffectType.SATURATION)
                && !LagLeniencies.hasHighLatency(player, LagLeniencies.averagePing); // Item
    }
}
