package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.MoveUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DetectionLocation {

    private static final Map<UUID, SpartanLocation>
            loc = Collections.synchronizedMap(new LinkedHashMap<>(Config.getMaxPlayers()));

    public static void run(SpartanPlayer p) {
        UUID uuid = p.getUniqueId();
        SpartanLocation old = loc.get(uuid);

        if (old != null) {
            SpartanLocation location = p.getLocation();
            double distance = location.distance(old);

            if (distance > (MoveUtils.chunk * 2)
                    && !p.getProfile().isHacker()) {
                update(p, location, true);
            } else if (distance >= 1.0 && !p.isDetected(true)) {
                update(p, location, false);
            }
        } else {
            update(p, p.getLocation(), true);
        }
    }

    public static void remove(SpartanPlayer p) {
        synchronized (loc) {
            loc.remove(p.getUniqueId());
        }
    }

    public static SpartanLocation get(SpartanPlayer p, boolean replaceNull) {
        synchronized (loc) {
            SpartanLocation location = loc.get(p.getUniqueId());
            return location != null
                    ? location
                    : (replaceNull ? p.getEventFromLocation() : null);
        }
    }

    public static void update(SpartanPlayer p, SpartanLocation location, boolean force) {
        if (force || !p.isDetected(true)) {
            int minHeight = BlockUtils.getMinHeight(p.getWorld());

            if (location.getBlockY() < minHeight) {
                location.setY(minHeight);
            }
            loc.put(p.getUniqueId(), location);
            p.getTimer().set("from-location=protection");
        }
    }
}
