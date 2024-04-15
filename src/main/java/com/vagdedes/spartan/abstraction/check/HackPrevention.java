package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;

public class HackPrevention {

    final String information;
    private final SpartanLocation location;
    private final boolean groundTeleport;
    boolean canPrevent;
    private final double damage;
    private final long expiration;
    final long time, tick;

    // Separator

    HackPrevention() {
        this.time = 0L;
        this.tick = 0L;
        this.expiration = 0L;
        this.information = "";
        this.location = null;
        this.groundTeleport = false;
        this.damage = 0.0;
        this.canPrevent = false;
    }

    HackPrevention(SpartanPlayer player, Enums.HackType hackType, String verbose,
                   SpartanLocation location, int cancelTicks, boolean groundTeleport,
                   double damage) {
        this.time = System.currentTimeMillis();
        this.tick = TPS.getTick(player);
        this.expiration = this.tick + cancelTicks;
        this.information = verbose;
        this.location = location;
        this.groundTeleport = groundTeleport;
        this.damage = damage;
        this.canPrevent = false;
        player.getViolations(hackType).run(this);
    }

    boolean hasExpired(long tick) {
        return tick > this.expiration;
    }

    void handle(SpartanPlayer player) {
        if (this.location != null) {
            player.safeTeleport(this.location);
        }
        if (this.groundTeleport) {
            player.groundTeleport(true);
        }
        if (this.damage > 0.0) {
            player.applyFallDamage(this.damage);
        }
    }
}
