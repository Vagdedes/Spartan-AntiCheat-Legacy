package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.abstraction.data.Decimals;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.identifiers.complex.unpredictable.Damage;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;

public class MovementProcessing {

    public static void run(SpartanPlayer p,
                           double dis, double hor, double ver, double box,
                           float fall) {
        // Damage
        Damage.runMove(p);

        // NMS Distance Caching
        p.setNmsDistance(dis, hor, ver, box, fall);

        // Jump/Fall Identifier
        if (Math.abs(PlayerUtils.jumping[1] - ver) < PlayerUtils.getJumpingPrecision(p)) { // Last Jump
            p.setLastJump();
        } else if (p.isFalling(ver)) { // Last Fall
            p.setLastFall();
        }

        // Extra Packets
        String key = "player-data=extra-packets";

        if (!p.isDetected(true)) {
            int maxTicks = 20;
            double difference = p.getCustomDistance() - dis;
            p.getDecimals().add(key, difference, maxTicks);

            if (p.getBuffer().increase(key, 1) >= maxTicks) {
                p.getBuffer().remove(key);

                if (p.getDecimals().get(key, Decimals.CALCULATE_AVERAGE) >= 0.01) {
                    p.setExtraPackets(p.getExtraPackets() + 1);
                } else {
                    p.setExtraPackets(0);
                }
            }
        } else { // Reset so it can again start from zero
            p.getBuffer().remove(key);
            p.getDecimals().remove(key);
        }
    }
}
