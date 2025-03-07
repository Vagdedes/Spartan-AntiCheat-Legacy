package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.IrregularMovements;
import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.server.Permissions;
import me.vagdedes.spartan.system.Enums;

public class ServerFlying {

    static void run(PlayerProtocol p) {
        CheckDetection detection = ((IrregularMovements) p.profile().getRunner(Enums.HackType.IrregularMovements)).limitServerFlying;

        detection.call(() -> {
            if (detection.isEnabled()
                    && !Permissions.isBypassing(p.bukkit(), Enums.HackType.IrregularMovements)
                    && p.bukkitExtra.movement.isFlying()
                    && !p.bukkitExtra.movement.isGliding()
                    && !p.bukkitExtra.movement.isSwimming()
                    && p.bukkitExtra.getVehicle() == null
                    && !p.bukkit().isSleeping()
                    && !p.bukkit().isDead()
                    && !p.trackers.has(PlayerTrackers.TrackerFamily.VELOCITY)
                    && !p.trackers.has(PlayerTrackers.TrackerFamily.MOTION)
                    && Attributes.getAmount(p, Attributes.GENERIC_FLYING_SPEED) == 0.0) {
                double limit = (p.bukkit().getFlySpeed() * 10.0) + 1.0,
                        nmsDistance = p.getLocation().distance(p.getFromLocation());

                if (nmsDistance >= limit
                        || SpartanLocation.distance(
                        p.getLocation(),
                        p.bukkitExtra.movement.getSchedulerFromLocation()
                ) >= limit) {
                    p.teleport(p.getFromLocation());
                }
            }
        });
    }

}
