package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion;

import com.vagdedes.spartan.utils.minecraft.entity.AxisAlignedBB;

public class MoveEvent {

    public MotionVector from, to, motion;
    private boolean toGround;
    private AxisAlignedBB aabbFrom;
    public boolean ignoreHorizontal, ignoreVertical, collidedHorizontal, collidedVertical;

    public MoveEvent(MotionVector from, MotionVector to, MotionVector motion, boolean toGround,
                     boolean isCollidedHorizontal, boolean isCollidedVertical, AxisAlignedBB aabbFrom) {
        this.from = from;
        this.to = to;
        this.motion = motion;
        this.toGround = toGround;
        this.collidedHorizontal = isCollidedHorizontal;
        this.collidedVertical = isCollidedVertical;
        this.aabbFrom = aabbFrom;
    }

    public void setIgnoreHorizontalCollision() {
        this.ignoreHorizontal = true;
    }

    public void setIgnoreVerticalCollision() {
        this.ignoreVertical = true;
    }

    public AxisAlignedBB getAABBFrom() {
        return this.aabbFrom;
    }

    public boolean toGround() {
        return this.toGround;
    }

    public MotionVector from() {
        return this.from;
    }

    public MotionVector to() {
        return this.to;
    }

    public MotionVector motion() {
        return this.motion;
    }

    public MotionVector getFrom() {
        return this.from;
    }

    public MotionVector getTo() {
        return this.to;
    }

    public MotionVector getMotion() {
        return this.motion;
    }

    public boolean isToGround() {
        return this.toGround;
    }

    public boolean isIgnoreHorizontal() {
        return this.ignoreHorizontal;
    }

    public boolean isIgnoreVertical() {
        return this.ignoreVertical;
    }

    public boolean isCollidedHorizontal() {
        return this.collidedHorizontal;
    }

    public boolean isCollidedVertical() {
        return this.collidedVertical;
    }

    public void setFrom(MotionVector from) {
        this.from = from;
    }

    public void setTo(MotionVector to) {
        this.to = to;
    }

    public void setMotion(MotionVector motion) {
        this.motion = motion;
    }

    public void setToGround(boolean toGround) {
        this.toGround = toGround;
    }

    public void setAabbFrom(AxisAlignedBB aabbFrom) {
        this.aabbFrom = aabbFrom;
    }

    public void setIgnoreHorizontal(boolean ignoreHorizontal) {
        this.ignoreHorizontal = ignoreHorizontal;
    }

    public void setIgnoreVertical(boolean ignoreVertical) {
        this.ignoreVertical = ignoreVertical;
    }

    public void setCollidedHorizontal(boolean collidedHorizontal) {
        this.collidedHorizontal = collidedHorizontal;
    }

    public void setCollidedVertical(boolean collidedVertical) {
        this.collidedVertical = collidedVertical;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof MoveEvent;
    }

    public String toString() {
        return "MoveEvent(from=" + this.getFrom() + ", to=" + this.getTo() + ", motion=" + this.getMotion() + ", toGround=" + this.isToGround() + ", ignoreHorizontal=" + this.isIgnoreHorizontal() + ", ignoreVertical=" + this.isIgnoreVertical() + ", collidedHorizontal=" + this.isCollidedHorizontal() + ", collidedVertical=" + this.isCollidedVertical() + ")";
    }
}
