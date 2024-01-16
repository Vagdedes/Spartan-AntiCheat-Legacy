package com.vagdedes.spartan.checks.player;

import com.vagdedes.spartan.abstraction.CheckExecutor;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.protections.LagLeniencies;
import com.vagdedes.spartan.objects.data.Buffer;
import com.vagdedes.spartan.objects.data.Decimals;
import com.vagdedes.spartan.objects.data.Timer;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.potion.PotionEffectType;

public class FastHeal extends CheckExecutor {

    private static final HackType check = HackType.FastHeal;
    private static final String checkID = Integer.toString(check.ordinal());

    public FastHeal(SpartanPlayer player) {
        super(check, player);
    }

    private long calculate() {
        boolean regeneration = player.hasPotionEffect(PotionEffectType.REGENERATION);
        long i = 0L;

        if (regeneration && !player.isOnFire()) {
            int level = PlayerData.getPotionLevel(player, PotionEffectType.REGENERATION);

            if (level <= 10) {
                i = level == 0 ? 300L : 45L;
            }
        } else if (!regeneration) {
            i = !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ? 3700L : 450L;
        }
        return i;
    }

    @Override
    public boolean handle(Object object) {
        if (object instanceof EntityRegainHealthEvent) {
            if (canDo()) {
                RegainReason reason = ((EntityRegainHealthEvent) object).getRegainReason();

                if (reason != RegainReason.CUSTOM
                        && reason != RegainReason.MAGIC) {
                    Timer timer = player.getTimer(hackType);
                    Decimals decimals = player.getDecimals(hackType);
                    double health = player.getHealth();

                    double previousHealth = player.getDecimals(hackType).get(checkID);
                    long time = player.getTimer(hackType).get(checkID);

                    if (timer.has(time) && decimals.has(previousHealth)
                            && previousHealth != health
                            && !LagLeniencies.hasHighLatency(player, LagLeniencies.mediocrePing)) { // Library
                        long limit = calculate();

                        if (limit > 0L) {
                            Buffer buffer = player.getBuffer(hackType);

                            if (time < limit && buffer.start(checkID, 10) >= 3) {
                                buffer.remove(checkID);
                                cancel("type: default, ms: " + time + ", limit: " + limit);
                            }
                        }
                    }
                    timer.set(checkID);
                    decimals.set(checkID, health);
                }
            }
        } else if (canDo() && ((FoodLevelChangeEvent) object).getFoodLevel() > player.getFoodLevel()
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                || player.getItemInHand().getType() != Material.DRIED_KELP)) {
            hackType.getCheck().addDisabledUser(player.getUniqueId(), hackType.toString(), 10);
        }
        return false;
    }

    @Override
    public boolean run() {
        return false;
    }

    @Override
    protected boolean canDo() {
        if (player.canRunCheck(hackType, true)
                && player.getWorld().getDifficulty() != Difficulty.PEACEFUL) {
            GameMode gameMode = player.getGameMode();
            return gameMode == GameMode.ADVENTURE || gameMode == GameMode.SURVIVAL;
        }
        return false;
    }
}
