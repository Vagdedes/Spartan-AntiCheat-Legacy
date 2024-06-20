package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.listeners.protocol.async.LagCompensation;

public class HitTime extends DetectionExecutor {

    HitTime(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    private int vl = 0;
    private int vlAnalysis = 0;
    private int nullPacket = 0;
    private int devianceTotal = 0;
    private int packets = 0;
    private int lowPackets = 0;

    private long lastHitTime = 0;
    private long oldHitTiming = 0;
    void runDamage() {
        if (!SpartanBukkit.packetsEnabled()) return;
        long hitTime = System.currentTimeMillis() - lastHitTime;
        int deviance = (int) Math.abs(hitTime - oldHitTiming);
        devianceTotal += deviance;
        packets += 1;
        if (player.getPing() > 1_000) return;
        if (deviance < 5 && deviance != 0) lowPackets++;
        if (hitTime == 0) {
            nullPacket = 2;
        }
        if (nullPacket > 1) {
            vl += 55;
            if (vl >= 300) {
                cancel("type: switch", (vl - 200) / 100D, player.movement.getEventFromLocation());
            }
        }
        if (packets >= 5 && LagCompensation.getPlayerTicksDelay(player.getInstance().getEntityId()) < 2) {
            if (devianceTotal < 35) {
                vlAnalysis += (devianceTotal < 10) ? 500 : 100;
                if (vlAnalysis >= 300) {
                    cancel("type: post(linear-analysis)", (vl - 200) / 100D, player.movement.getEventFromLocation());
                }
            } else if (devianceTotal > 45 && devianceTotal < 55) {
                vlAnalysis += 150;
                if (vlAnalysis >= 300) {
                    cancel("type: post(default)", (vl - 200) / 100D, player.movement.getEventFromLocation());
                }
            } else if (lowPackets >= 3) {
                vlAnalysis += 100;
                if (vlAnalysis >= 300) {
                    cancel("type: post(low)", (vl - 200) / 100D, player.movement.getEventFromLocation());
                }
            } else {
                if (vlAnalysis > 0) vlAnalysis -= 50;
            }
            if (vlAnalysis >= 300) {
                cancel("type: post(improbable)", vl / 100D, player.movement.getEventFromLocation());
                vlAnalysis -= 50;
            }
            packets = 0;
            devianceTotal = 0;
            lowPackets = 0;
        }
        if (vl > 0) vl -= 5;

        if (nullPacket > 0) nullPacket -= 1;
        lastHitTime = System.currentTimeMillis();
        oldHitTiming = hitTime;
    }
}
