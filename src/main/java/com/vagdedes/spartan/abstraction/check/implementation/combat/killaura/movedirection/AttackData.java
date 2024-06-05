package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

import java.util.UUID;

public class AttackData {

    private final double accuracy;
    private final float yaw, pitch;
    private final long time;
    private final UUID target;

    AttackData(double accuracy, float yaw, float pitch, long time, UUID target) {
        this.accuracy = accuracy;
        this.yaw = yaw;
        this.pitch = pitch;
        this.time = time;
        this.target = target;
    }

    double getAccuracy() {
        return accuracy;
    }

    float getYaw() {
        return yaw;
    }

    float getPitch() {
        return pitch;
    }
    long getTime() { return time; }
    UUID getTarget() {
        return target;
    }
}
