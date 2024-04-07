package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    private final Map<PlayerProfile, LinearPatternProfile> profiles;
    private final Map<Long, Collection<Double>> correlations;
    private final Collection<Double> empty;

    protected LinearPatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        super(pattern, initiator);
        this.profiles = new LinkedHashMap<>();
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.empty = new ArrayList<>(0);
    }

    @Override
    protected PatternProfile removeProfileInternal(PlayerProfile profile) {
        return this.profiles.remove(profile);
    }

    @Override
    protected boolean hasSituation(long situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

    @Override
    protected boolean customLearn(PlayerProfile profile, long situation, double pattern) {
        LinearPatternProfile patternProfile = this.getProfile(profile);

        if (patternProfile.repetition.canUse(pattern)) {
            synchronized (this.correlations) {
                this.customCache(this.correlations, this.initiator.cube, situation, pattern);
            }
            this.addToSignificance(pattern);

            synchronized (patternProfile.correlations) {
                this.customCache(patternProfile.correlations, this.initiator.line, situation, pattern);
            }
            synchronized (patternProfile.lastCorrelation) {
                patternProfile.lastCorrelation.put(situation, pattern);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void deleteLessons(long situation, double pattern) {
        synchronized (this.correlations) {
            Collection<Double> list = this.correlations.get(situation);

            if (list != null
                    && list.remove(pattern)
                    && list.isEmpty()) {
                this.correlations.remove(situation);
            }
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

    @Override
    protected void clearProfiles() {
        this.profiles.clear();
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
