package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.java.HashUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPredictor;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    private static final class Cache {

        private ProbabilityPredictor predictor;
        private long expiration;

        private Cache(ProbabilityPredictor predictor) {
            this.refresh(predictor);
        }

        private boolean isExpired() {
            return this.expiration < System.currentTimeMillis();
        }

        private ProbabilityPredictor refresh(ProbabilityPredictor predictor) {
            this.predictor = predictor;
            this.expiration = System.currentTimeMillis() + 1_000L;
            return predictor;
        }
    }

    private final Map<PlayerProfile, LinearPatternProfile> profiles;
    private final Map<Integer, Collection<Float>> correlations;
    private final int initialHash;
    private final Map<Integer, Cache> cache;

    protected LinearPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.profiles = new HashMap<>();
        this.correlations = Collections.synchronizedMap(new HashMap<>());
        this.initialHash = Objects.hash(pattern.key.hashCode(), System.currentTimeMillis());
        this.cache = new HashMap<>();
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
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

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
    protected boolean compareInternal(PlayerProfile profile, long time, int situation) {
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.isLoaded()) {
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
            int hash = HashUtils.extendInt(this.initialHash, situation);
            hash = HashUtils.extendInt(hash, Float.hashCode(current));

            synchronized (this.correlations) {
                Cache cache = this.cache.get(hash);

                if (cache == null) {
                    Collection<Float> collection = this.correlations.get(situation);

                    if (collection != null) {
                        if (this.cache.size() == Pattern.individualGlobalDataLimit) {
                            Iterator<Integer> iterator = this.cache.keySet().iterator();
                            iterator.next();
                            iterator.remove();
                        }
                        predictor = new ProbabilityPredictor(collection, current);
                        this.cache.put(hash, new Cache(predictor));
                    } else {
                        return false;
                    }
                } else if (cache.isExpired()) {
                    Collection<Float> collection = this.correlations.get(situation);

                    if (collection != null) {
                        predictor = cache.refresh(new ProbabilityPredictor(collection, current));
                    } else {
                        this.cache.remove(hash);
                        return false;
                    }
                } else {
                    predictor = cache.predictor;
                }
            }
            patternProfile.updateComparison(
                    time,
                    this.significance(after) * this.significance(before),
                    predictor.afterPie.getProbability(after) * predictor.beforePie.getProbability(before),
                    predictor.afterPie.getRawTotal()
            );
            return true;
        } else {
            return false;
        }
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

    @Override
    public PatternProfile getProfile(PlayerProfile profile) {
        return this.getLocalProfile(profile);
    }

    // Separator

    private LinearPatternProfile getLocalProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> new LinearPatternProfile(this, profile)
            );
        }
    }

}
