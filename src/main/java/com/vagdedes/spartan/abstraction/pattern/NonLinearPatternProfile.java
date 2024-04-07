package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

public class NonLinearPatternProfile extends PatternProfile {

    NonLinearPatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        super(patternGeneralization, profile);
    }

    @Override
    void deleteCustomData() {
    }

}
