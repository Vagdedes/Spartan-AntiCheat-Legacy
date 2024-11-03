package com.vagdedes.spartan.abstraction.check.implementation.player;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Buffer;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
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

    private final DetectionExecutor detection;
    private double previousHealth;
    private long time;
    private final Buffer.IndividualBuffer buffer;

    public FastHeal(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedDetectionExecutor(this, null, true);
        this.buffer = new Buffer.IndividualBuffer();
    }

    private long calculate() {
        int level = PlayerUtils.getPotionLevel(this.protocol().spartanPlayer, PotionEffectType.REGENERATION);
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
                double health = this.protocol().spartanPlayer.getHealth();
                long timePassed = System.currentTimeMillis() - time;
                time = System.currentTimeMillis();

                if (previousHealth != health) {
                    long limit = calculate();

                    if (limit > 0L) {
                        if (timePassed < limit
                                && buffer.count(1, 10) == 3) {
                            buffer.reset();
                            this.detection.cancel(
                                    "type: default, ms: " + timePassed + ", limit: " + limit,
                                    limit / (double) timePassed
                            );
                        }
                    }
                }
                previousHealth = health;
            }
        } else if (((FoodLevelChangeEvent) object).getFoodLevel() > this.protocol().spartanPlayer.getInstance().getFoodLevel()
                && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                || this.protocol().spartanPlayer.getItemInHand().getType() != Material.DRIED_KELP)) {
            this.protocol().spartanPlayer.getExecutor(hackType).addDisableCause(null, null, 10);
        }
    }

    @Override
    protected boolean canRun() {
        if (this.protocol().spartanPlayer.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && this.protocol().spartanPlayer.getInstance().getFireTicks() <= 0
                && Attributes.getAmount(this.protocol().spartanPlayer, Attributes.GENERIC_MAX_ABSORPTION) == 0.0) {
            GameMode gameMode = this.protocol().spartanPlayer.getInstance().getGameMode();
            return gameMode == GameMode.ADVENTURE || gameMode == GameMode.SURVIVAL;
        } else {
            return false;
        }
    }
}
