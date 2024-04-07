package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.functionality.server.TPS;

public class PatternRepetition {

    private double number;
    private short count;

    PatternRepetition() {
        this.number = Double.MIN_VALUE;
        this.count = 0;
    }

    boolean canUse(double number) {
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
