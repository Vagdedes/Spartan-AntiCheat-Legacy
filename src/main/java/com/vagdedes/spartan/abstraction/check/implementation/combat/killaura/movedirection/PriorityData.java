package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

public class PriorityData {

    private final float vlLimit;
    private final float vlFader;

    public PriorityData(float vlLimit, float vlFader) {
        this.vlLimit = vlLimit;
        this.vlFader = vlFader;
    }

    public float getVlLimit() {
        return vlLimit;
    }

    public float getVlFader() {
        return vlFader;
    }
}
