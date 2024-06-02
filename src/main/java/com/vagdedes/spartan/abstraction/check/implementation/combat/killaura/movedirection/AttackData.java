package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

public class AttackData {

    private final double accuracy;
    private final float yaw, pitch;

    AttackData(double accuracy, float yaw, float pitch) {
        this.accuracy = accuracy;
        this.yaw = yaw;
        this.pitch = pitch;
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
}
