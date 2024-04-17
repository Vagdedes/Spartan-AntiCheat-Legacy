package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LinearPatternProfile extends PatternProfile {

    final List<Float> correlations;

    LinearPatternProfile(PlayerProfile profile) {
        super(profile);
        this.correlations = Collections.synchronizedList(new LinkedList<>());
    }

}
