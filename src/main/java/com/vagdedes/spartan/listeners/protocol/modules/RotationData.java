package com.vagdedes.spartan.listeners.protocol.modules;

import com.vagdedes.spartan.functionality.tracking.RayLine;

import com.vagdedes.spartan.functionality.tracking.RayLine;

public class RotationData {

    private final float yaw;
    private final float pitch;

    public RotationData(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float yaw() { return this.yaw; }
    public float pitch() { return this.pitch; }
}
