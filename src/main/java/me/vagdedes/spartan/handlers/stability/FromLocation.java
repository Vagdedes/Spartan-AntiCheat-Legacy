package me.vagdedes.spartan.handlers.stability;

import me.vagdedes.spartan.configuration.Config;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FromLocation {

    private static final Map<UUID, SpartanLocation> loc = new ConcurrentHashMap<>(Config.getMaxPlayers());

    public static void run(SpartanPlayer p) {
        UUID uuid = p.getUniqueId();
        SpartanLocation old = loc.get(uuid);

        if (old != null) {
            double distance = p.getNmsDistance();

            if (distance >= 1.0
                    && (distance > (MoveUtils.chunk * 2)
                    || p.getTimer().get("from-location=protection") > 10_000L
                    || !Check.hasViolations(uuid)
                    || Check.getLastViolation(p, false) > 1_000L)) {
                update(p, p.getLocation(), false);
            }
        } else {
            update(p, p.getLocation(), false);
        }
    }

    public static void remove(SpartanPlayer p) {
        loc.remove(p.getUniqueId());
    }

    public static SpartanLocation get(SpartanPlayer p, boolean replaceNull) {
        SpartanLocation location = loc.get(p.getUniqueId());
        return location != null ? location : (replaceNull ? MoveUtils.getCachedLocation(p) : null);
    }

    public static void update(SpartanPlayer p, SpartanLocation location, boolean force) {
        if (location.getBlockY() > BlockUtils.getMinHeight(p.getWorld())
                && (force || !Moderation.wasDetected(p))) {
            loc.put(p.getUniqueId(), location);
            p.getTimer().set("from-location=protection");
        }
    }
}
