package com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion;

public class KeyBind {

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    public KeyBind(boolean up, boolean down, boolean left, boolean right) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
    }
    public boolean isNone() {
        return !this.up && !this.down && !this.left && !this.right;
    }
    public boolean is(boolean up, boolean down, boolean left, boolean right) {
        return this.up == up && this.down == down && this.left == left && this.right == right;
    }
}
