package me.vagdedes.spartan.objects.system;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.api.ViolationResetEvent;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.gui.info.PlayerInfo;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPrevention;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LiveViolation {

    private static final ConcurrentHashMap<LiveViolation, String> maxLevel = new ConcurrentHashMap<>();

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (maxLevel.size() > 0) {
                    for (Map.Entry<LiveViolation, String> entry : maxLevel.entrySet()) {
                        LiveViolation violations = entry.getKey();
                        SpartanPlayer player = violations.getPlayer();

                        if (player != null) {
                            Enums.HackType hackType = violations.getHackType();

                            if (violations.getLevel() < Check.maxViolations) {
                                new HackPrevention(player, hackType, entry.getValue());
                            } else {
                                maxLevel.remove(violations);
                            }
                        } else {
                            maxLevel.remove(violations);
                        }
                    }
                }
            }, 1L, 1L);
        }
    }

    private final UUID uuid;
    private final Enums.HackType hackType;
    private long cycleExpiration, lastViolation, cancelledLevel;
    private int level;
    private boolean cancelled;

    public LiveViolation(Enums.HackType hackType) {
        this.uuid = SpartanBukkit.uuid;
        this.hackType = hackType;
        this.level = 0;
        this.cancelledLevel = 0L;
        this.lastViolation = 0L;
        this.cancelled = false;
    }

    public LiveViolation(UUID uuid, Enums.HackType hackType) {
        this.uuid = uuid;
        this.hackType = hackType;
        this.level = 0;
        this.cancelledLevel = 0L;
        this.lastViolation = 0L;
        this.cancelled = false;
        reset(); // Do not make it local as it takes part in object initiation and is not important
    }

    // Separator

    public int getLevel() {
        timeReset();
        return level;
    }

    private int getRawCancelledLevel() {
        long time = cancelledLevel - System.currentTimeMillis();
        return time > 0L ? AlgebraUtils.integerRound(time / 1000.0) : 0;
    }

    public int getCancelledLevel() {
        return Math.min(getRawCancelledLevel(), hackType.getCheck().getMaxCancelledViolations());
    }

    public boolean isLastLevelCancelled() {
        return cancelled;
    }

    public boolean hasLevel() {
        return getLevel() > 0;
    }

    public boolean hasMaxLevel() {
        return getLevel() == Check.maxViolations;
    }

    public boolean hasCancelledLevel() {
        return getRawCancelledLevel() > 0;
    }

    public boolean hasMaxCancelledLevel() {
        return getRawCancelledLevel() >= hackType.getCheck().getMaxCancelledViolations();
    }

    public void setLevel(int amount) {
        timeReset();
        int previousLevel = level;

        // Violations
        this.level = Math.min(
                Check.maxViolations,
                amount
        );
        this.cancelled = false;
        timeReset(); // Always after changing the level

        // Always last
        if (previousLevel != level) {
            SpartanPlayer player = getPlayer();

            if (player != null) {
                PlayerInfo.refresh(player.getName());
            }
        }
    }

    public void increaseCancelledLevel() {
        long time = System.currentTimeMillis();

        if (cancelledLevel < time) {
            this.cancelledLevel = time + 1_000L;
        } else {
            this.cancelledLevel += 1_000L;
        }
        this.cancelled = true;
    }

    public void setMaxLevel(String message) {
        maxLevel.put(this, message);
    }

    public void removeMaxLevel(String message) {
        String cached = maxLevel.get(this);

        if (cached != null && cached.equals(message)) {
            maxLevel.remove(this);
        }
    }

    // Separator

    public void reset() {
        reset(false);
    }

    private void reset(boolean local) {
        ViolationResetEvent event;

        if (SpartanBukkit.isSynchronised()) {
            SpartanPlayer spartanPlayer = this.getPlayer();

            if (spartanPlayer != null) {
                Player player = spartanPlayer.getPlayer();

                if (player != null && player.isOnline()) {
                    if (Settings.getBoolean("Important.enable_developer_api")) {
                        event = new ViolationResetEvent(player, this.getHackType());
                        Register.manager.callEvent(event);
                    } else {
                        event = null;
                    }
                } else {
                    event = null;
                }
            } else {
                event = null;
            }
        } else {
            event = null;
        }

        if (event == null || !event.isCancelled()) {
            maxLevel.remove(this);
            this.level = 0;
            this.cancelled = false;
        } else if ((System.currentTimeMillis() - this.cycleExpiration) > Check.violationCycleSeconds) { // Forceful reset to keep potential developer API usage within hardcoded thresholds
            maxLevel.remove(this);
            this.level = 0;
            this.cancelled = false;
        }
        this.cycleExpiration = System.currentTimeMillis() + Check.violationCycleSeconds;

        // Always last
        SpartanPlayer player = getPlayer();

        if (player != null) {
            player.getProfile().resetLiveEvidence(hackType);

            if (local) {
                PlayerInfo.refresh(player.getName());
            }
        }
    }

    private void timeReset() {
        long time = System.currentTimeMillis();

        if (this.cycleExpiration < time) {
            reset(true);
        }
    }

    // Separator

    public long getLastViolation(boolean maxIfNull) {
        return maxIfNull && this.lastViolation == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - this.lastViolation;
    }

    public void setLastViolation() {
        this.lastViolation = System.currentTimeMillis();
    }

    // Separator

    public Enums.HackType getHackType() {
        return hackType;
    }

    public UUID getUUID() {
        return uuid;
    }

    public SpartanPlayer getPlayer() {
        return SpartanBukkit.getPlayer(this.uuid);
    }
}
