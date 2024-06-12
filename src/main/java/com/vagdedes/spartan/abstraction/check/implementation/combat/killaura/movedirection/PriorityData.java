package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

public class PriorityData {

    private final float vlLimit, vlFader;

    PriorityData(float vlLimit, float vlFader) {
        this.vlLimit = vlLimit;
        this.vlFader = vlFader;
    }

    float getVlLimit() {
        return vlLimit;
    }

    float getVlFader() {
        return vlFader;
    }
}
