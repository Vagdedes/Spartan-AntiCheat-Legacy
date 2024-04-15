package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;

public class HackPrevention {

    final String information;
    private final SpartanLocation location;
    private final boolean groundTeleport;
    private final double damage;
    long stay;
    private final long expiration;
    final long time, tick;

    // Separator

    HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose,
                   SpartanLocation location, int cancelTicks, boolean groundTeleport,
                   double damage) {
        this.time = System.currentTimeMillis();
        this.tick = TPS.getTick(player);
        this.stay = cancelTicks;
        this.expiration = cancelTicks == 0 ? 0L : this.tick + this.stay;
        this.information = verbose;
        this.location = location;
        this.groundTeleport = groundTeleport;
        this.damage = damage;
        player.getViolations(hackType).run(this);
    }

    boolean hasExpired(long tick) {
        return tick > this.expiration;
    }

    boolean handle(SpartanPlayer player) {
        SpartanLocation preventionFrom = this.location;
        boolean result = false;

        if (preventionFrom != null) {
            result = player.safeTeleport(preventionFrom);
        }
        if (this.groundTeleport) {
            result |= player.groundTeleport(true);
        }
        if (damage > 0.0) {
            result |= player.applyFallDamage(damage);
        }
        return result;
    }
}
