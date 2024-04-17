package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

public abstract class PatternProfile {

    final int hash;
    final PatternRepetition repetition;

    PatternProfile(PlayerProfile profile) {
        this.hash = Pattern.hash(profile);
        this.repetition = new PatternRepetition();
    }

}
