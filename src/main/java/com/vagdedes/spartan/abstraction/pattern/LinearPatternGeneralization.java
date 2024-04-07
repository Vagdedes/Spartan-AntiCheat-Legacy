package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    final Map<PlayerProfile, LinearPatternProfile> profiles;
    final Map<Long, Collection<Double>> correlations;
    final Collection<Double> empty;

    protected LinearPatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        super(pattern, initiator);
        this.profiles = new LinkedHashMap<>();
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.empty = new ArrayList<>(0);
    }

    @Override
    protected boolean hasSituation(long situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

    // Separator

    @Override
    protected void customCache(PatternProfile patternProfile, long situation, double pattern) {
        synchronized (this.correlations) {
            this.customCache(this.correlations, this.initiator.cube, situation, pattern);
        }
        synchronized (((LinearPatternProfile) patternProfile).correlations) {
            this.customCache(((LinearPatternProfile) patternProfile).correlations, this.initiator.line, situation, pattern);
        }
        synchronized (((LinearPatternProfile) patternProfile).lastCorrelation) {
            ((LinearPatternProfile) patternProfile).lastCorrelation.put(situation, pattern);
        }
    }

    private void customCache(Map<Long, Collection<Double>> map,
                             int limit,
                             long situation,
                             double pattern) {
        Collection<Double> list = map.computeIfAbsent(
                situation,
                k -> new ArrayList<>()
        );

        if (list.size() == limit) {
            if (!(list instanceof LinkedList)) {
                list = new LinkedList<>(list);
                map.put(situation, list);
            }
            ((LinkedList<Double>) list).removeFirst();
        }
        list.add(pattern);
    }

    // Separator

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

    public Collection<Double> getLinearData(long situation) {
        synchronized (this.correlations) {
            return new ArrayList<>(this.correlations.getOrDefault(situation, this.empty));
        }
    }

    public LinearPatternProfile getProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> new LinearPatternProfile(this, profile)
            );
        }
    }

}
