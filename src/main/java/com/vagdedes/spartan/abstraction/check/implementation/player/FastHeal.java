package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.potion.PotionEffectType;

public class FastHeal extends CheckExecutor {

    private double previousHealth;
    private long time;
    private final Buffer.IndividualBuffer buffer;

    public FastHeal(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.previousHealth = player.getInstance().getHealth();
        this.buffer = new Buffer.IndividualBuffer();
    }

    private long calculate() {
        int level = PlayerUtils.getPotionLevel(player, PotionEffectType.REGENERATION);
        long i = 0L;

        if (level > -1) {
            if (level <= 10) {
                i = level == 0 ? 300L : 45L;
            }
        } else {
            i = !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ? 3700L : 450L;
        }
        return i;
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof EntityRegainHealthEvent) {
            RegainReason reason = ((EntityRegainHealthEvent) object).getRegainReason();

            if (reason != RegainReason.CUSTOM
                    && reason != RegainReason.MAGIC) {
                double health = player.getInstance().getHealth();
                long timePassed = System.currentTimeMillis() - time;
                time = System.currentTimeMillis();

                if (previousHealth != health) {
                    long limit = calculate();

                    if (limit > 0L) {
                        if (timePassed < limit
                                && buffer.count(1, 10) == 3) {
                            buffer.reset();
                            cancel("type: default, ms: " + timePassed + ", limit: " + limit);
                        }
                    }
                }
                previousHealth = health;
            }
        } else if (((FoodLevelChangeEvent) object).getFoodLevel() > player.getInstance().getFoodLevel()
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                || player.getItemInHand().getType() != Material.DRIED_KELP)) {
            player.getExecutor(hackType).addDisableCause(hackType.toString(), null, 10);
        }
    }

    @Override
    protected boolean canRun() {
        if (player.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && player.getInstance().getFireTicks() <= 0) {
            GameMode gameMode = player.getInstance().getGameMode();
            return gameMode == GameMode.ADVENTURE || gameMode == GameMode.SURVIVAL;
        } else {
            return false;
        }
    }
}
