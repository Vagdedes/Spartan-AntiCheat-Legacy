package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.functionality.server.TPS;

public class PatternRepetition {

    private Number number;
    private int count;

    PatternRepetition(Number number) {
        this.number = number;
        this.count = 1;
    }

    boolean canUse(Number number) {
        if (this.number.doubleValue() != number.doubleValue()) {
            count = 1;
            this.number = number;
            return true;
        } else {
            count++;
            return count <= TPS.tickTimeInteger;
        }
    }
}
