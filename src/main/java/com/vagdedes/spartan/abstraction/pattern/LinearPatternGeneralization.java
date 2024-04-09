package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.java.HashUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPredictor;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    private static final class Cache {

        private ProbabilityPredictor predictor;
        private int dataSize;
        private long expiration;

        private Cache(ProbabilityPredictor predictor, int dataSize) {
            this.refresh(predictor, dataSize);
        }

        private boolean isExpired() {
            return this.expiration < System.currentTimeMillis();
        }

        private ProbabilityPredictor refresh(ProbabilityPredictor predictor, int dataSize) {
            this.predictor = predictor;
            this.dataSize = dataSize;
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
        this.cache = new HashMap<>(Pattern.individualGlobalDataLimitBase, 1.0f);
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
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.repetition.canUse(pattern)) {
            synchronized (this.correlations) {
                Collection<Float> list = this.correlations.computeIfAbsent(
                        situation,
                        k -> new ArrayList<>(Pattern.individualGlobalDataLimitBase)
                );
                if (list.size() == Pattern.individualGlobalDataLimit - 1) {
                    if (!(list instanceof LinkedList)) {
                        list = new LinkedList<>(list);
                        this.correlations.put(situation, list);
                    }
                    this.decreaseImportance(
                            situation,
                            patternProfile,
                            ((LinkedList<Float>) list).removeFirst()
                    );
                }
                list.add(pattern);
            }

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
            return patternProfile;
        } else {
            return null;
        }
    }

    @Override
    protected boolean customCompare(PlayerProfile profile, long time, int situation) {
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.isLoaded()) {
            float after, pattern, before;

            synchronized (patternProfile.correlations) {
                LinkedList<Float> list = patternProfile.correlations.get(situation);

                if (list != null && list.size() >= 3) {
                    after = list.getLast();
                    pattern = list.get(list.size() - 2);
                    before = list.get(list.size() - 3);
                } else {
                    return false;
                }
            }
            Cache cache;
            int hash = HashUtils.extendInt(this.initialHash, situation);
            hash = HashUtils.extendInt(hash, Float.hashCode(pattern));

            synchronized (this.correlations) {
                cache = this.cache.get(hash);

                if (cache == null) {
                    Collection<Float> collection = this.correlations.get(situation);

                    if (collection == null) {
                        cache = new Cache(new ProbabilityPredictor(), 0);
                    } else {
                        cache = new Cache(new ProbabilityPredictor(collection, pattern), collection.size());

                        if (this.cache.size() == Pattern.individualGlobalDataLimit - 1) {
                            Iterator<Integer> iterator = this.cache.keySet().iterator();
                            iterator.next();
                            iterator.remove();
                        }
                        this.cache.put(hash, cache);
                    }
                } else if (cache.isExpired()) {
                    Collection<Float> collection = this.correlations.get(situation);

                    if (collection == null) {
                        cache = new Cache(new ProbabilityPredictor(), 0);
                        this.cache.remove(hash);
                    } else {
                        cache.refresh(new ProbabilityPredictor(collection, pattern), collection.size());
                    }
                }
            }
            patternProfile.finishComparison(
                    time,
                    profile,
                    situation,
                    pattern,
                    cache.predictor.afterPie.getRatio(after, Pattern.defaultRatio)
                            * cache.predictor.beforePie.getRatio(before, Pattern.defaultRatio),
                    cache.dataSize
            );
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void customDeletePattern(int situation, float pattern) {
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
