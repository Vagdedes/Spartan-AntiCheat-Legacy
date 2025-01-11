package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.listeners.protocol.standalone.LegacyLagCompensationListener;

public class HitTime extends ProbabilityDetection {

    HitTime(CheckRunner executor) {
        super(executor, null, Check.DetectionType.PACKETS, "hit_time", true);
    }

    private int
            vl = 0,
            vlAnalysis = 0,
            nullPacket = 0,
            devianceTotal = 0,
            packets = 0,
            lowPackets = 0;

    private long
            lastHitTime = 0,
            oldHitTiming = 0;

    void run() {
        this.call(() -> {
            long hitTime = System.currentTimeMillis() - lastHitTime;
            int deviance = (int) Math.abs(hitTime - oldHitTiming);
            devianceTotal += deviance;
            packets += 1;

            if (this.protocol.getPing() > 1_000) {
                return;
            }
            if (deviance < 5 && deviance != 0) {
                lowPackets++;
            }
            if (hitTime == 0) {
                nullPacket = 2;
            }
            if (nullPacket > 1) {
                vl += 55;
                if (vl >= 300) {
                    cancel("type: switch", this.protocol.getFromLocation());
                }
            }
            if (packets >= 5 && LegacyLagCompensationListener.getPlayerTicksDelay(this.protocol.spartan.getEntityId()) < 2) {
                if (devianceTotal < 35) {
                    vlAnalysis += (devianceTotal < 10) ? 150 : 75;
                    if (vlAnalysis >= 300) {
                        cancel("type: post(linear-analysis)", this.protocol.getFromLocation());
                    }
                } else if (devianceTotal > 45 && devianceTotal < 55) {
                    vlAnalysis += 150;
                    if (vlAnalysis >= 300) {
                        cancel("type: post(default)", this.protocol.getFromLocation());
                    }
                } else if (lowPackets >= 3) {
                    vlAnalysis += 100;
                    if (vlAnalysis >= 300) {
                        cancel("type: post(low)", this.protocol.getFromLocation());
                    }
                } else {
                    if (vlAnalysis > 0) vlAnalysis -= 50;
                }
                packets = 0;
                devianceTotal = 0;
                lowPackets = 0;
            }
            if (vl > 0) {
                vl -= 5;
            }
            if (nullPacket > 0) {
                nullPacket -= 1;
            }
            lastHitTime = System.currentTimeMillis();
            oldHitTiming = hitTime;
        });
    }

}
