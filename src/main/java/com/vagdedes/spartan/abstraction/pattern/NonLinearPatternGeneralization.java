package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NonLinearPatternGeneralization extends PatternGeneralization {

    final Map<PlayerProfile, NonLinearPatternProfile> profiles;
    final Map<Long, ProbabilityPie> correlations;
    private final ProbabilityPie empty;

    protected NonLinearPatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        super(pattern, initiator);
        this.profiles = new LinkedHashMap<>();
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.empty = new ProbabilityPie(0);
    }

    @Override
    protected boolean hasSituation(long situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

    @Override
    protected void customCache(PatternProfile patternProfile, long situation, double pattern) {
        synchronized (this.correlations) {
            ProbabilityPie pie = this.correlations.computeIfAbsent(
                    situation,
                    k -> new ProbabilityPie()
            );

            if (pie.getSlices() == this.initiator.cube) {
                pie.removeLeastSignificant();
            }
            pie.add(pattern);
        }
    }

    @Override
    protected void clearProfiles() {
        this.profiles.clear();
    }

    @Override
    protected PatternProfile removeProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.remove(profile);
        }
    }

    @Override
    protected Collection<? extends PatternProfile> getProfiles() {
        return this.profiles.values();
    }

    // Separator

    public double[] getProbabilityAndTotal(long situation, double client) {
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
