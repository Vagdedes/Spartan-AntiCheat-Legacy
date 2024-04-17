package com.vagdedes.spartan.utils.math;

import com.vagdedes.spartan.abstraction.math.implementation.NumberMath;

import java.util.Collection;

public class ProbabilityPredictor {

    public final NumberMath before, after;

    public ProbabilityPredictor() {
        this.before = new NumberMath(0);
        this.after = new NumberMath(0);
    }

    public ProbabilityPredictor(Collection<Float> totalPatterns, float currentPattern) {
        if (totalPatterns.size() > 1) {
            this.before = new NumberMath();
            this.after = new NumberMath();
            Float found = null;
            float previous = Float.MIN_VALUE;

            for (float pattern : totalPatterns) {
                if (found != null) {
                    this.before.add(found);
                    this.after.add(pattern);

                    if (currentPattern == pattern) {
                        found = previous;
                    } else {
                        found = null;
                    }
                } else if (currentPattern == pattern) {
                    found = previous;
                }
                previous = pattern;
            }
        } else {
            this.before = new NumberMath(0);
            this.after = new NumberMath(0);
        }
    }

}