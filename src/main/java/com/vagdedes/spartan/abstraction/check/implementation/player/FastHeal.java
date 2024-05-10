package com.vagdedes.spartan.abstraction.check.implementation.player;

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

    public FastHeal(SpartanPlayer player) {
        super(HackType.FastHeal, player);
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
    public void handleInternal(boolean cancelled, Object object) {
        if (object instanceof EntityRegainHealthEvent) {
            if (canDo()) {
                RegainReason reason = ((EntityRegainHealthEvent) object).getRegainReason();

                if (reason != RegainReason.CUSTOM
                        && reason != RegainReason.MAGIC) {
                    Timer timer = this.getTimer();
                    Decimals decimals = this.getDecimals();
                    double health = player.getHealth();

                    double previousHealth = this.getDecimals().get("");
                    long time = this.getTimer().get("");

                    if (timer.has(time) && decimals.has(previousHealth)
                            && previousHealth != health) {
                        long limit = calculate();

                        if (limit > 0L) {
                            Buffer buffer = this.getBuffer();

                            if (time < limit
                                    && buffer.count("", 10) == 3) {
                                buffer.remove("");
                                cancel("type: default, ms: " + time + ", limit: " + limit);
                            }
                        }
                    }
                    timer.set("");
                    decimals.set("", health);
                }
            }
        } else if (canDo() && ((FoodLevelChangeEvent) object).getFoodLevel() > player.getFoodLevel()
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                || player.getItemInHand().getType() != Material.DRIED_KELP)) {
            player.getViolations(hackType).addDisableCause(hackType.toString(), null, 10);
        }
    }

    @Override
    public void runInternal(boolean cancelled) {

    }

    @Override
    public void schedulerInternal() {

    }

    @Override
    protected boolean canDo() {
        if (player.getWorld().getDifficulty() != Difficulty.PEACEFUL) {
            GameMode gameMode = player.getGameMode();
            return gameMode == GameMode.ADVENTURE || gameMode == GameMode.SURVIVAL;
        } else {
            return false;
        }
    }
}
