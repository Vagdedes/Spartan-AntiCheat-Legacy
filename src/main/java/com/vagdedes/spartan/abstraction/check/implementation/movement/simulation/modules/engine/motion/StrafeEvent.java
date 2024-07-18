package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion;

public final class StrafeEvent {

    private float friction;
    private MotionVector relative;
    private float yaw;

    public StrafeEvent(float friction, MotionVector relative, float yaw) {
        this.friction = friction;
        this.relative = relative;
        this.yaw = yaw;
    }

    /*
    public void applyMotion(double speed, float strafeMotion, float horizontalMotion) {
        float remainder = 1 - strafeMotion;
        strafeMotion *= 0.91;
        if (mc.player.isOnGround()) {
            Movement.setSpeed(speed);
        } else {
            mc.player.getMotion().x = mc.player.getMotion().getX() * strafeMotion;
            mc.player.getMotion().z = mc.player.getMotion().getZ() * strafeMotion;
            friction = (float) speed * remainder;
        }
    }

     */

    public float getFriction() {
        return this.friction;
    }

    public MotionVector getRelative() {
        return this.relative;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    public void setRelative(MotionVector relative) {
        this.relative = relative;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public String toString() {
        return "StrafeEvent(friction=" + this.getFriction() + ", relative=" + this.getRelative() + ", yaw=" + this.getYaw() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof StrafeEvent)) return false;
        final StrafeEvent other = (StrafeEvent) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        if (Float.compare(this.getFriction(), other.getFriction()) != 0) return false;
        final Object this$relative = this.getRelative();
        final Object other$relative = other.getRelative();
        if (this$relative == null ? other$relative != null : !this$relative.equals(other$relative)) return false;
        if (Float.compare(this.getYaw(), other.getYaw()) != 0) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof StrafeEvent;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = result * PRIME + Float.floatToIntBits(this.getFriction());
        final Object $relative = this.getRelative();
        result = result * PRIME + ($relative == null ? 43 : $relative.hashCode());
        result = result * PRIME + Float.floatToIntBits(this.getYaw());
        return result;
    }
}