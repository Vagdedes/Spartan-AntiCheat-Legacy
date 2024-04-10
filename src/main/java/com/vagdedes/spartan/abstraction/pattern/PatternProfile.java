package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

public abstract class PatternProfile {

    final int hash;
    private final PatternGeneralization generalization;
    final PatternRepetition repetition;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.hash = Pattern.hash(profile);
        this.generalization = patternGeneralization;
        this.repetition = new PatternRepetition();
    }

    // Separator

    protected final void finishComparison(PlayerProfile profile,
                                          int situation,
                                          float pattern,
                                          double probability,
                                          int dataSize) {
        double existingContribution = (1.0 - AlgebraUtils.contribution(dataSize))
                        * (1.0 - this.generalization.uniqueContribution(situation, pattern));

        if (this.generalization.parent.key.endsWith("vertical")) {
            profile.getSpartanPlayer().debug(
                    false,
                    false,
                    1.0 - (probability * existingContribution)
            );
        }
    }

    // Separator

    abstract void deleteCustomData();

}
