package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinearPatternProfile extends PatternProfile {

    final List<Float> correlations;
    int previousSituation;

    LinearPatternProfile(PlayerProfile profile) {
        super(profile);
        this.correlations = Collections.synchronizedList(new ArrayList<>(4));
        this.previousSituation = 0;
    }

}
