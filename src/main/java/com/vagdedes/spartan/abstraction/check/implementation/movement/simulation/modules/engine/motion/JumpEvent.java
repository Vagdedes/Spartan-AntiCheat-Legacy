package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion;

public class JumpEvent {
    private float yaw;

    public JumpEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public String toString() {
        return "JumpEvent(yaw=" + this.getYaw() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof JumpEvent)) return false;
        final JumpEvent other = (JumpEvent) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        if (Float.compare(this.getYaw(), other.getYaw()) != 0) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof JumpEvent;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = result * PRIME + Float.floatToIntBits(this.getYaw());
        return result;
    }
}
