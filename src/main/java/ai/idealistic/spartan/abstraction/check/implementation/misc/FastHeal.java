package ai.idealistic.spartan.abstraction.check.implementation.misc;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.data.Buffer;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.compatibility.manual.vanilla.Attributes;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.potion.PotionEffectType;

public class FastHeal extends CheckRunner {

    private final CheckDetection detection;
    private double previousHealth;
    private long time;
    private final Buffer.IndividualBuffer buffer;

    public FastHeal(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedDetection(this, null, null, null, true);
        this.buffer = new Buffer.IndividualBuffer();
    }

    // EXTENSION: Rebuilt to use perfect Vanilla Ticks-to-MS conversion
    private long calculate() {
        int level = PlayerUtils.getPotionLevel(this.protocol, PotionEffectType.REGENERATION);

        if (level > -1) {
            // Vanilla Formula: 50 ticks (Regen 1), halved per additional level.
            // (50 >> 0) = 50 | (50 >> 1) = 25 | (50 >> 2) = 12
            int vanillaTicks = Math.max(1, 50 >> level);
            long expectedMs = vanillaTicks * 50L;

            // Subtract a 150ms buffer to safely absorb server tick-rate jitter
            return Math.max(50L, expectedMs - 150L);
        } else {
            // Pre-1.9 natural healing: 80 ticks (4000ms) -> Safe limit 3500ms
            // 1.9+ Saturation healing: 10 ticks (500ms) -> Safe limit 400ms
            return !MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ? 3500L : 400L;
        }
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof EntityRegainHealthEvent) {
            this.detection.call(() -> {
                EntityRegainHealthEvent event = (EntityRegainHealthEvent) object;
                RegainReason reason = event.getRegainReason();

                if (reason != RegainReason.CUSTOM
                        && reason != RegainReason.MAGIC) {
                    double health = this.protocol.getHealth();
                    long timePassed = System.currentTimeMillis() - time;
                    time = System.currentTimeMillis();

                    if (previousHealth != health) {
                        long limit = calculate();

                        if (limit > 0L) {
                            if (timePassed < limit) {
                                // EXTENSION: Dynamic lag-resistant amplitude buffer.
                                // Blatant packet spam (e.g. 10ms between heals) fills the buffer instantly.
                                // Mild jitter (e.g. 380ms) requires repeated offenses to flag.
                                long safeTime = Math.max(timePassed, 1L);
                                double amplitude = Math.min((double) limit / safeTime, 5.0);

                                if (buffer.count(amplitude, 20) >= 15) {
                                    buffer.reset();
                                    this.detection.cancel(
                                            "default, ms: " + timePassed + ", limit: " + limit
                                    );

                                    if (this.prevent()) {
                                        event.setCancelled(true);
                                    }
                                }
                            } else {
                                // Natural buffer decay for legitimate players
                                buffer.decrease(0.5);
                            }
                        }
                    }
                    previousHealth = health;
                }
            });
        } else if (object instanceof FoodLevelChangeEvent) {
            if (((FoodLevelChangeEvent) object).getFoodLevel() > this.protocol.bukkit().getFoodLevel()
                    && (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                    || this.protocol.getItemInHand().getType() != Material.DRIED_KELP)) {
                this.protocol.getRunner(hackType).addDisableCause(null, null, 10);
            }
        }
    }

    @Override
    protected boolean canRun() {
        if (this.protocol.getWorld().getDifficulty() != Difficulty.PEACEFUL
                && this.protocol.bukkit().getFireTicks() <= 0
                && Attributes.getAmount(this.protocol, Attributes.GENERIC_MAX_ABSORPTION) == Double.MIN_VALUE) {
            GameMode gameMode = this.protocol.getGameMode();
            return gameMode == GameMode.ADVENTURE || gameMode == GameMode.SURVIVAL;
        } else {
            return false;
        }
    }
}