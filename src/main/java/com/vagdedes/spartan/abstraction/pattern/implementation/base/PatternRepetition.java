package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.functionality.server.TPS;

public class PatternRepetition {

    private float number;
    private int count;

    PatternRepetition() {
        this.number = Float.MIN_VALUE;
        this.count = 0;
    }

    boolean canUse(float number) {
        if (this.number != number) {
            this.count = 1;
            this.number = number;
            return true;
        } else {
            this.count++;
            return this.count <= TPS.maximum;
        }
    }
}
