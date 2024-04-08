package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    private final Map<PlayerProfile, LinearPatternProfile> profiles;
    private final Map<Integer, Collection<Float>> correlations;
    private final Collection<Float> empty;

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
                int delete = list.size() - Pattern.individualGlobalDataLimit;

                if (delete > 0) {
                    if (!(list instanceof LinkedList)) {
                        list = new LinkedList<>(list);
                        this.correlations.put(situation, list);
                    }
                    for (int i = 0; i < delete; i++) {
                        this.decreaseSignificance(((LinkedList<Float>) list).removeFirst());
                    }
                }
                list.add(pattern);
            }
            this.increaseSignificance(pattern);

            synchronized (patternProfile.correlations) {
                LinkedList<Float> list = patternProfile.correlations.computeIfAbsent(
                        situation,
                        k -> new LinkedList<>()
                );
                int delete = list.size() - 2;

                if (delete > 0) {
                    for (int i = 0; i < delete; i++) {
                        this.decreaseSignificance(list.removeFirst());
                    }
                }
                list.add(pattern);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void compareInternal(PlayerProfile playerProfile, long time, int situation) {

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
