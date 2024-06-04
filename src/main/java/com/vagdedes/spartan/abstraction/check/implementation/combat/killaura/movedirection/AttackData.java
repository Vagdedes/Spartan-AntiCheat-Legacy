package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

public class AttackData {

    private final double accuracy;
    private final float yaw, pitch;
    private final long time;

    AttackData(double accuracy, float yaw, float pitch, long time) {
        this.accuracy = accuracy;
        this.yaw = yaw;
        this.pitch = pitch;
        this.time = time;
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
}
