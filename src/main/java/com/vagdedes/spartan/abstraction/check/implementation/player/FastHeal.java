package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.data.Timer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
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
        int level = PlayerUtils.getPotionLevel(player, PotionEffectType.REGENERATION);
        long i = 0L;

        if (level > -1) {
            if (!player.isOnFire()) {
                if (level <= 10) {
                    i = level == 0 ? 300L : 45L;
                }
            }
        } else {
            i = !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ? 3700L : 450L;
        }
        return i;
    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
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
                            && previousHealth != health) {
                        long limit = calculate();

                        if (limit > 0L) {
                            Buffer buffer = player.getBuffer(hackType);

                            if (time < limit && buffer.start(checkID, 10, Check.detectionMeasurementTicks) >= 0.3) {
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
            player.getViolations(hackType).addDisableCause(hackType.toString(), null, 10);
        }
        return false;
    }

    @Override
    public boolean runInternal(boolean cancelled) {
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
