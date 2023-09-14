package me.vagdedes.spartan.checks.implementation.player;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class FastEat {

    private static final HackType check = HackType.FastEat;
    private static final Material cake = MaterialUtils.get("cake");

    private static final long delay = 1000L;
    private static final long driedKelp = 550L;
    private static final int ticks = 25;

    public static void runFood(SpartanPlayer p, int lvl) {
        if (!canDo(p) || lvl <= p.getFoodLevel()) {
            return;
        }
        if (checkDelay(p) || checkEating(p)) {
            Cooldowns cooldowns = p.getCooldowns(check);
            p.setFoodLevel(p.getFoodLevel() - (lvl - p.getFoodLevel()), true);

            if (!cooldowns.canDo("regeneration")) {
                p.removePotionEffect(PotionEffectType.REGENERATION);
            }
            if (!cooldowns.canDo("absorption")) {
                p.removePotionEffect(PotionEffectType.ABSORPTION);
            }
            if (!cooldowns.canDo("resistance")) {
                p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            }
        }
    }

    private static long getDelay(SpartanPlayer p) {
        for (ItemStack itemStack : new ItemStack[]{p.getItemInHand(), p.getInventory().getItemInOffHand()}) {
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

    private static boolean checkDelay(SpartanPlayer p) {
        boolean bool;
        Timer timer = p.getTimer(check);
        long time = timer.get("time");
        long delay = getDelay(p);

        if (bool = (delay > 0L && time <= delay)) {
            new HackPrevention(p, check, "type: eat, ms: " + time);
        }
        timer.set("time");
        return bool;
    }

    private static boolean checkEating(SpartanPlayer p) {
        long time = p.getTimer(check).get("food");
        long delay = getDelay(p);

        if (delay > 0L && time <= delay) {
            new HackPrevention(p, check, "type: interact, ms: " + time + ", delay: " + delay);
            return true;
        }
        return false;
    }

    public static void runInteract(SpartanPlayer p, SpartanBlock b, Action action) {
        if (!p.isBedrockPlayer() && canDo(p)) {
            if (action == Action.RIGHT_CLICK_BLOCK && b.getType() == cake) {
                check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), 10);
            } else if (action == Action.RIGHT_CLICK_AIR && PlayerData.holdsFood(p)) {
                goldenAppleEffects(p);
                p.getTimer(check).set("food");
            }
        }
    }

    private static void goldenAppleEffects(SpartanPlayer p) {
        ItemStack item = p.getItemInHand();

        if (item != null && item.getType() == Material.GOLDEN_APPLE) {
            if (!p.hasPotionEffect(PotionEffectType.REGENERATION)) {
                p.getCooldowns(check).add("regeneration", ticks);
            }
            if (!p.hasPotionEffect(PotionEffectType.ABSORPTION)) {
                p.getCooldowns(check).add("absorption", ticks);
            }
            if (!p.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                p.getCooldowns(check).add("resistance", ticks);
            }
        }
    }

    private static boolean canDo(SpartanPlayer p) {
        return p.canRunCheck(check, true) && p.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && !p.hasPotionEffect(PotionEffectType.SATURATION) && !LagLeniencies.hasHighLatency(p, LagLeniencies.averagePing); // Item
    }
}
