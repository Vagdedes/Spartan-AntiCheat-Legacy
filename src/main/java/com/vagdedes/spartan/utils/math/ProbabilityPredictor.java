package com.vagdedes.spartan.utils.math;

import java.util.Collection;

public class ProbabilityPredictor { // todo add cache based on search

    public final ProbabilityPie beforePie, afterPie;

    public ProbabilityPredictor(Collection<Float> totalPatterns, float currentPattern) {
        this.beforePie = new ProbabilityPie();
        this.afterPie = new ProbabilityPie();

        if (currentPattern != Float.MIN_VALUE
                && totalPatterns.size() > 1) {
            Float found = null;
            float previous = Float.MIN_VALUE;

            for (float pattern : totalPatterns) {
                if (found != null) {
                    this.beforePie.add(found);
                    this.afterPie.add(pattern);

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
        }
    }

}