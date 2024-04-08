package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.ProbabilityPredictor;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    private final Map<PlayerProfile, LinearPatternProfile> profiles;
    private final Map<Integer, Collection<Float>> correlations;

    protected LinearPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.profiles = new LinkedHashMap<>();
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
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
        LinearPatternProfile patternProfile = this.getProfile(profile);

        if (patternProfile.repetition.canUse(pattern)) {
            synchronized (this.correlations) {
                Collection<Float> list = this.correlations.computeIfAbsent(
                        situation,
                        k -> new ArrayList<>()
                );
                if (list.size() == Pattern.individualGlobalDataLimit) {
                    if (!(list instanceof LinkedList)) {
                        list = new LinkedList<>(list);
                        this.correlations.put(situation, list);
                    }
                    this.decreaseSignificance(((LinkedList<Float>) list).removeFirst());
                }
                list.add(pattern);
            }
            this.increaseSignificance(pattern);

            synchronized (patternProfile.correlations) {
                LinkedList<Float> list = patternProfile.correlations.computeIfAbsent(
                        situation,
                        k -> new LinkedList<>()
                );
                if (list.size() == 3) {
                    list.removeFirst();
                }
                list.add(pattern);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean compareInternal(PlayerProfile playerProfile, long time, int situation) {
        LinearPatternProfile patternProfile = this.getProfile(playerProfile);
        float after, current, before;

        synchronized (patternProfile.correlations) {
            LinkedList<Float> list = patternProfile.correlations.get(situation);

            if (list != null && list.size() >= 3) {
                after = list.getLast();
                current = list.get(list.size() - 2);
                before = list.get(list.size() - 3);
            } else {
                return false;
            }
        }
        ProbabilityPredictor predictor;

        synchronized (this.correlations) {
            Collection<Float> list = this.correlations.get(situation);

            if (list != null) {
                predictor = new ProbabilityPredictor(list, current);
            } else {
                return false;
            }
        }
        patternProfile.updateComparison(
                time,
                this.significance(after) * this.significance(before),
                predictor.afterPie.getProbability(after) * predictor.beforePie.getProbability(before),
                predictor.afterPie.getRawTotal()
        );
        return true;
    }

    @Override
    protected void deleteCustomLessonsInternal(int situation, float pattern) {
        synchronized (this.correlations) {
            Collection<Float> list = this.correlations.get(situation);

            if (list != null
                    && list.remove(pattern)
                    && list.isEmpty()) {
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

    private LinearPatternProfile getProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> new LinearPatternProfile(this, profile)
            );
        }
    }

}
