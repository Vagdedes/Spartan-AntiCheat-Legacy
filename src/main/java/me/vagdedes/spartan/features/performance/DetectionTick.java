package me.vagdedes.spartan.features.performance;

import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.data.Cooldowns;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.LiveViolation;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;

import java.util.UUID;

public class DetectionTick {

    private long futureTime, tick;

    public DetectionTick() {
        this.futureTime = 0L;
    }

    public boolean canSkip(UUID uuid, PlayerProfile profile, Cooldowns cooldowns, int ticksCooldown, SpartanPlayer player) {
        boolean canSkip;
        long currentTime = System.currentTimeMillis();

        if (currentTime > futureTime) {
            this.futureTime = currentTime + (ticksCooldown * TPS.tickTime);
            this.tick = TPS.getTick(player);
            canSkip = true;
        } else {
            canSkip = this.tick == TPS.getTick(player);
        }

        if (canSkip
                && !MaximumCheckedPlayers.isActive()
                && !FalsePositiveDetection.isTimedOut(cooldowns)
                && !profile.isSuspectedOrHacker()
                && !player.hasNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance)) {
            for (Enums.HackType hackType : Enums.hackTypes()) {
                LiveViolation liveViolation = hackType.getCheck().getViolations(uuid);

                if (liveViolation.getLastViolation(true) <= 10_000L) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
