package me.vagdedes.spartan.checks.implementation.player;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.data.Decimals;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.utils.gameplay.PlayerData;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.potion.PotionEffectType;

public class FastHeal {

    private static final HackType check = HackType.FastHeal;
    private static final String checkID = Integer.toString(check.ordinal());

    private static long calculate(SpartanPlayer p) {
        boolean regeneration = p.hasPotionEffect(PotionEffectType.REGENERATION);
        long i = 0L;

        if (regeneration && !p.isOnFire()) {
            int level = PlayerData.getPotionLevel(p, PotionEffectType.REGENERATION);

            if (level <= 10) {
                i = level == 0 ? 300L : 45L;
            }
        } else if (!regeneration) {
            i = !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ? 3700L : 450L;
        }
        return i;
    }

    public static void run(SpartanPlayer p, RegainReason reason) {
        if (!canDo(p) || reason == RegainReason.CUSTOM || reason == RegainReason.MAGIC) {
            return;
        }
        Timer timer = p.getTimer(check);
        Decimals decimals = p.getDecimals(check);
        double health = p.getHealth();

        double previousHealth = p.getDecimals(check).get(checkID);
        long time = p.getTimer(check).get(checkID);

        if (timer.has(time) && decimals.has(previousHealth)
                && previousHealth != health
                && !LagLeniencies.hasHighLatency(p, LagLeniencies.mediocrePing)) { // Library
            long limit = calculate(p);

            if (limit > 0L) {
                Buffer buffer = p.getBuffer(check);

                if (time < limit && buffer.start(checkID, 10) >= 3) {
                    buffer.remove(checkID);
                    new HackPrevention(p, check, "type: default, ms: " + time + ", limit: " + limit);
                }
            }
        }
        timer.set(checkID);
        decimals.set(checkID, health);
    }

    public static void runFood(SpartanPlayer p, int lvl) {
        if (canDo(p) && lvl > p.getFoodLevel()
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) || p.getItemInHand().getType() != Material.DRIED_KELP)) {
            check.getCheck().addDisabledUser(p.getUniqueId(), check.toString(), 10);
        }
    }

    private static boolean canDo(SpartanPlayer p) {
        if (p.canRunCheck(check, true) && p.getWorld().getDifficulty() != Difficulty.PEACEFUL) {
            GameMode gameMode = p.getGameMode();
            return gameMode == GameMode.ADVENTURE || gameMode == GameMode.SURVIVAL;
        }
        return false;
    }
}
