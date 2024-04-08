package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NonLinearPatternGeneralization extends PatternGeneralization {

    private final Map<PlayerProfile, NonLinearPatternProfile> profiles;
    private final Map<Integer, ProbabilityPie> correlations;
    private final ProbabilityPie empty;

    protected NonLinearPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.profiles = new LinkedHashMap<>();
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.empty = new ProbabilityPie(0);
    }

    @Override
    protected PatternProfile removeProfileInternal(PlayerProfile profile) {
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
    protected boolean customLearn(PlayerProfile profile, int situation, float pattern) {
        NonLinearPatternProfile patternProfile = this.getProfile(profile);

        if (patternProfile.repetition.canUse(pattern)) {
            synchronized (this.correlations) {
                ProbabilityPie pie = this.correlations.computeIfAbsent(
                        situation,
                        k -> new ProbabilityPie()
                );
                int delete = pie.getSlices() - Pattern.individualGlobalDataLimit;

                if (delete > 0) {
                    for (int i = 0; i < delete; i++) {
                        this.decreaseSignificance(pie.removeLeastSignificant());
                    }
                }
                pie.add(pattern);
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
            ProbabilityPie pie = this.correlations.get(situation);

            if (pie != null
                    && pie.remove(pattern)
                    && pie.isEmpty()) {
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

    public double[] getProbabilityAndTotal(int situation, double client) {
        synchronized (this.correlations) {
            ProbabilityPie pie = this.correlations.getOrDefault(situation, this.empty);
            return new double[]{
                    pie.getProbability(client),
                    pie.getRawTotal()
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
