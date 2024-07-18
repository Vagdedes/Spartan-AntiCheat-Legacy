package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion;

public class MotionVector {

    public static MotionVector ZERO = new MotionVector(0, 0, 0);

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
    public MotionVector normalize() {
        double d0 = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        return d0 < 1.0E-4D ? ZERO : new MotionVector(this.x / d0, this.y / d0, this.z / d0);
    }
    /**
     * Euclidean distance between this and the specified vector, returned as double.
     */
    public double distanceTo(MotionVector vec) {
        double d0 = vec.x - this.x;
        double d1 = vec.y - this.y;
        double d2 = vec.z - this.z;
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    /**
     * The square of the Euclidean distance between this and the specified vector.
     */
    public double squareDistanceTo(MotionVector vec) {
        double d0 = vec.x - this.x;
        double d1 = vec.y - this.y;
        double d2 = vec.z - this.z;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public double squareDistanceTo(double xIn, double yIn, double zIn) {
        double d0 = xIn - this.x;
        double d1 = yIn - this.y;
        double d2 = zIn - this.z;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public MotionVector scale(double factor) {
        return this.mul(factor, factor, factor);
    }

    public MotionVector inverse() {
        return this.scale(-1.0D);
    }

    public MotionVector mul(MotionVector vec) {
        return this.mul(vec.x, vec.y, vec.z);
    }

    public MotionVector mul(double factorX, double factorY, double factorZ) {
        return new MotionVector(this.x * factorX, this.y * factorY, this.z * factorZ);
    }

    /**
     * Returns the length of the vector.
     */
    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public double lengthSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }
}

