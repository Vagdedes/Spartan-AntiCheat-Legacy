package com.vagdedes.spartan.abstraction.pattern;

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
        } else if (this.count < Pattern.individualDataLimit) {
            this.count++;
            return true;
        } else {
            return false;
        }
    }
}
