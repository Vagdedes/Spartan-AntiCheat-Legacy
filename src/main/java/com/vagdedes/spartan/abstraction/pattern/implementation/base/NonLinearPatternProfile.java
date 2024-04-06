package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NonLinearPatternProfile extends PatternProfile {

    final Map<Long, ProbabilityPie> correlations;

    NonLinearPatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        super(patternGeneralization, profile);
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    @Override
    void deleteCustomData() {
        synchronized (this.correlations) {
            this.correlations.clear();
        }
    }

}
