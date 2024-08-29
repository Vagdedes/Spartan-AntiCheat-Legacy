package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import me.vagdedes.spartan.system.Enums;

public class HackPrevention {

    boolean canPrevent;
    private final long threadID;
    private final SpartanLocation location;
    private final boolean groundTeleport;
    private final double damage;
    private final long expiration;

    // Separator

    HackPrevention() {
        this.threadID = 0L;
        this.canPrevent = false;
        this.location = null;
        this.groundTeleport = false;
        this.damage = 0.0;
        this.expiration = 0L;
    }

    HackPrevention(SpartanPlayer player, Enums.HackType hackType,
                   String information, double violations,
                   SpartanLocation location, int cancelTicks, boolean groundTeleport,
                   double damage) {
        long time = System.currentTimeMillis();
        this.threadID = Thread.currentThread().getId();
        this.canPrevent = false;
        this.location = location;
        this.groundTeleport = groundTeleport;
        this.damage = damage;
        this.expiration = cancelTicks <= 1 ? Long.MIN_VALUE
                : System.currentTimeMillis() + (cancelTicks * TPS.tickTime);
        player.getExecutor(hackType).violate(this, information, violations, time);
    }

    private boolean runsOnPackets() {
        return this.threadID == SpartanBukkit.packetsThread.getID();
    }

    private boolean isOneTime() {
        return this.expiration == Long.MIN_VALUE;
    }

    boolean complete() {
        if (this.canPrevent) {
            if (this.isOneTime()) {
                this.canPrevent = false;
            } else if (System.currentTimeMillis() > this.expiration) {
                return false;
            }
            return !runsOnPackets();
        }
        return false;
    }

    void handle(SpartanPlayer player) {
        if (this.location != null) {
            player.teleport(this.location);
        }
        if (this.groundTeleport) {
            player.groundTeleport();
        }
        if (this.damage > 0.0
                && (this.location == null && !this.groundTeleport
                || Config.settings.getBoolean("Detections.fall_damage_on_teleport"))) {
            player.damage(this.damage);
        }
    }

}
