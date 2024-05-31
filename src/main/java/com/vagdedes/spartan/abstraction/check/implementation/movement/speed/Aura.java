package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;


import com.vagdedes.spartan.utils.math.RayLine;

public class Aura {

    private final float yaw;
    private final RayLine rayLine;
    private final RayLine simRayLine;
    private boolean lookAtTarget;

    Aura(float yaw, RayLine rayLine, RayLine simRayLine, boolean lookAtTarget) {
        this.yaw = yaw;
        this.rayLine = rayLine;
        this.simRayLine = simRayLine;
        this.lookAtTarget = lookAtTarget;
    }

    float yaw() { return this.yaw; }
    RayLine rayLine() { return this.rayLine; }
    RayLine simRayLine() { return  this.simRayLine; }
    boolean isLookAtTarget() { return  this.lookAtTarget; }
}
