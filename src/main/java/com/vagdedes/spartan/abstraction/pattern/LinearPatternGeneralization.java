package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.ProbabilityPredictor;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    private final Map<PlayerProfile, LinearPatternProfile> profiles;
    private final Map<Long, Collection<Double>> correlations;
    private final Collection<Double> empty;

    protected LinearPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.profiles = new LinkedHashMap<>();
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.empty = new ArrayList<>(0);
    }

    @Override
    protected PatternProfile removeProfileInternal(PlayerProfile profile) {
        return this.profiles.remove(profile);
    }

    @Override
    protected boolean hasPatterns(long situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

    @Override
    protected boolean customLearn(PlayerProfile profile, long situation, double pattern) {
        LinearPatternProfile patternProfile = this.getProfile(profile);

        if (patternProfile.repetition.canUse(pattern)) {
            synchronized (this.correlations) {
                this.manageData(this.correlations, this.parent.limit(), situation, pattern);
            }
            this.addToSignificance(pattern);

            synchronized (patternProfile.correlations) {
                this.manageData(patternProfile.correlations, Pattern.individualDataLimit, situation, pattern);
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
    protected void deleteCustomLessons(long situation, double pattern) {
        synchronized (this.correlations) {
            Collection<Double> list = this.correlations.get(situation);

            if (list != null
                    && list.remove(pattern)
                    && list.isEmpty()) {
                this.correlations.remove(situation);
            }
        }
    }

    private void manageData(Map<Long, Collection<Double>> map,
                            int limit,
                            long situation,
                            double pattern) {
        Collection<Double> list = map.computeIfAbsent(
                situation,
                k -> new ArrayList<>()
        );
        int delete = list.size() - limit;

        if (delete > 0) {
            if (!(list instanceof LinkedList)) {
                list = new LinkedList<>(list);
                map.put(situation, list);
            }
            for (int i = 0; i < delete; i++) {
                ((LinkedList<Double>) list).removeFirst();
            }
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

    public ProbabilityPredictor getLinearDataPrediction(LinearPatternProfile patternProfile, long situation, short count) {
        synchronized (this.correlations) {
            return ProbabilityPredictor.build(
                    situation,
                    this.correlations.getOrDefault(situation, this.empty),
                    patternProfile,
                    count
            );
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
