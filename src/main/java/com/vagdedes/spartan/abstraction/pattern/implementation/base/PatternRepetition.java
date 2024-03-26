package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.functionality.server.TPS;

public class PatternRepetition {

    private Number number;
    private int count;

    PatternRepetition() {
        this.number = Double.MIN_VALUE;
        this.count = 0;
    }

    boolean canUse(Number number) {
        if (this.number.doubleValue() != number.doubleValue()) {
            this.count = 1;
            this.number = number;
            return true;
        } else {
            this.count++;
            return this.count <= TPS.maximum;
        }
    }
}
