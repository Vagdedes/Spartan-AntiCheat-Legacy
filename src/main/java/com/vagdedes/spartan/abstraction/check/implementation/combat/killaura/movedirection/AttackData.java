package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

public class AttackData {

    private final double accuracy;
    private final float yaw;
    private final float pitch;

    public AttackData(double accuracy, float yaw, float pitch) {
        this.accuracy = accuracy;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
