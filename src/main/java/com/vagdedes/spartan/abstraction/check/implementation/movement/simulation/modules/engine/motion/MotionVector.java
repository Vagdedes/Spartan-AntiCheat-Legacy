package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion;

public class MotionVector {

    public double x;
    public double y;
    public double z;

    public MotionVector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
    }

    public MotionVector addWithClone(double x, double y, double z) {
        MotionVector v = this.cloneVector();
        v.add(x, y, z);
        return v;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public MotionVector cloneVector() {
        return new MotionVector(this.x, this.y, this.z);
    }
}

