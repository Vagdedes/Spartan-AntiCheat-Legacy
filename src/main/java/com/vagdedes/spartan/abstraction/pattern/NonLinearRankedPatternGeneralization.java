package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NonLinearRankedPatternGeneralization extends PatternGeneralization {

    private final Map<PlayerProfile, NonLinearPatternProfile> profiles;
    private final Map<Integer, ProbabilityRank> correlations;

    protected NonLinearRankedPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.profiles = new HashMap<>();
        this.correlations = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    protected PatternProfile customDeleteProfile(PlayerProfile profile) {
        return this.profiles.remove(profile);
    }

    @Override
    protected boolean hasPatterns() {
        return !this.correlations.isEmpty();
    }

    @Override
    protected boolean hasPatterns(int situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

    @Override
    protected PatternProfile customLearn(PlayerProfile profile, int situation, float pattern) {
        NonLinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.repetition.canUse(pattern)) {
            synchronized (this.correlations) {
                ProbabilityRank rank = this.correlations.computeIfAbsent(
                        situation,
                        k -> new ProbabilityRank()
                );
                if (rank.getParts() == Pattern.individualGlobalDataLimit) {
                    this.decreaseImportance(
                            situation,
                            patternProfile,
                            rank.removeLeastSignificant()
                    );
                }
                rank.add(pattern);
            }
            synchronized (patternProfile.correlations) {
                patternProfile.correlations.put(situation, pattern);
            }
            return patternProfile;
        } else {
            return null;
        }
    }

    @Override
    protected boolean customCompare(PlayerProfile profile, long time, int situation) {
        NonLinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.isLoaded()) {
            float pattern;

            synchronized (patternProfile.correlations) {
                Float cache = patternProfile.correlations.get(situation);

                if (cache != null) {
                    pattern = cache;
                } else {
                    return false;
                }
            }
            synchronized (this.correlations) {
                ProbabilityRank rank = this.correlations.get(situation);

                if (rank == null) {
                    patternProfile.finishComparison(
                            profile,
                            time,
                            this.significance(pattern),
                            Pattern.defaultRatio,
                            0.0
                    );
                } else {
                    patternProfile.finishComparison(
                            profile,
                            time,
                            this.significance(pattern),
                            rank.getPosition(pattern, Pattern.defaultPosition)
                                    * rank.getRatio(pattern, Pattern.defaultRatio),
                            (1.0 - AlgebraUtils.contribution(rank.getRawTotal()))
                                    * (1.0 - this.uniqueContribution(situation, pattern))
                    );
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void customDeletePattern(int situation, float pattern) {
        synchronized (this.correlations) {
            ProbabilityRank rank = this.correlations.get(situation);

            if (rank != null
                    && rank.remove(pattern)
                    && rank.isEmpty()) {
                this.correlations.remove(situation);
            }
        }
    }

    @Override
    protected void clearProfiles() {
        this.profiles.clear();
    }

    @Override
    protected Collection<? extends PatternProfile> getProfiles() {
        return this.profiles.values();
    }

    @Override
    public PatternProfile getProfile(PlayerProfile profile) {
        return this.getLocalProfile(profile);
    }

    // Separator

    private NonLinearPatternProfile getLocalProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> new NonLinearPatternProfile(this, profile)
            );
        }
    }

}
