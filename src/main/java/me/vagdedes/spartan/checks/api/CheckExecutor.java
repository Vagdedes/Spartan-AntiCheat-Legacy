package me.vagdedes.spartan.checks.api;

import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.HackPrevention;
import me.vagdedes.spartan.system.Enums;

public abstract class CheckExecutor {

    protected final Enums.HackType hackType;
    protected final SpartanPlayer player;

    public CheckExecutor(Enums.HackType hackType, SpartanPlayer player) {
        this.hackType = hackType;
        this.player = player;
    }

    abstract public void run();

    abstract public boolean canDo();

    protected final void cancel(SpartanPlayer player, Enums.HackType hackType, String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport, double damage, int violation) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, damage, violation);
    }

    protected final void cancel(SpartanPlayer player, Enums.HackType hackType, String verbose, SpartanLocation location, int cancelTicks,
                                boolean groundTeleport, double damage) {
        new HackPrevention(player, hackType, verbose, location, cancelTicks, groundTeleport, damage, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location, int teleportCooldown, boolean cancelTicks, double damage) {
        new HackPrevention(player, hackType, verbose, location, teleportCooldown, cancelTicks, damage, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location, int teleportCooldown, boolean cancelTicks) {
        new HackPrevention(player, hackType, verbose, location, teleportCooldown, cancelTicks, 0.0, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location, int teleportCooldown) {
        new HackPrevention(player, hackType, verbose, location, teleportCooldown, false, 0.0, 1);
    }

    protected final void cancel(String verbose, SpartanLocation location) {
        new HackPrevention(player, hackType, verbose, location, 0, false, 0.0, 1);
    }

    protected final void cancel(String verbose) {
        new HackPrevention(player, hackType, verbose, null, 0, false, 0.0, 1);
    }
}
