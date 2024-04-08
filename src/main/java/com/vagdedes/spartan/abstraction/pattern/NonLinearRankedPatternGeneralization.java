package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NonLinearRankedPatternGeneralization extends PatternGeneralization {

    private final Map<PlayerProfile, NonLinearPatternProfile> profiles;
    private final Map<Integer, ProbabilityRank> correlations;
    private final ProbabilityRank empty;

    protected NonLinearRankedPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.profiles = new LinkedHashMap<>();
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.empty = new ProbabilityRank();
    }

    @Override
    protected PatternProfile removeProfileInternal(PlayerProfile profile) {
        return this.profiles.remove(profile);
    }

    @Override
    protected boolean hasPatterns(int situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

    @Override
    protected boolean customLearn(PlayerProfile profile, int situation, float pattern) {
        NonLinearPatternProfile patternProfile = this.getProfile(profile);

        if (patternProfile.repetition.canUse(pattern)) {
            synchronized (this.correlations) {
                ProbabilityRank rank = this.correlations.computeIfAbsent(
                        situation,
                        k -> new ProbabilityRank()
                );
                int delete = rank.getParts() - Pattern.individualGlobalDataLimit;

                if (delete > 0) {
                    for (int i = 0; i < delete; i++) {
                        this.decreaseSignificance(rank.removeLeastSignificant());
                    }
                }
                rank.add(pattern);
            }
            this.increaseSignificance(pattern);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void deleteCustomLessonsInternal(int situation, float pattern) {
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

    // Separator

    public double[] getProbabilityAndPositionAndTotal(int situation, double client) {
        synchronized (this.correlations) {
            ProbabilityRank rank = this.correlations.getOrDefault(situation, this.empty);
            return new double[]{
                    rank.getProbability(client),
                    rank.getPosition(client),
                    rank.getRawTotal()
            };
        }
    }

    public double[] getProbabilityAndTotal(int situation, double client) {
        synchronized (this.correlations) {
            ProbabilityRank rank = this.correlations.getOrDefault(situation, this.empty);
            return new double[]{
                    rank.getProbability(client),
                    rank.getRawTotal()
            };
        }
    }

    public NonLinearPatternProfile getProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> new NonLinearPatternProfile(this, profile)
            );
        }
    }

}
