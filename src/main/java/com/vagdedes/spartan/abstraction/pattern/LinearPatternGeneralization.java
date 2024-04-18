package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.java.HashUtils;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    private static int[] getHashCodes(float[] patterns) {
        int nowHash = Float.hashCode(patterns[1]);
        return new int[]{
                HashUtils.extendInt(nowHash, Float.hashCode(patterns[0])),
                HashUtils.extendInt(nowHash, Float.hashCode(patterns[2]))
        };
    }

    private static final class Cache {

        private double beforeProbability, afterProbability;
        private long expiration;

        private Cache(double beforeProbability, double afterProbability) {
            this.refresh(beforeProbability, afterProbability);
        }

        private boolean isExpired() {
            return this.expiration < System.currentTimeMillis();
        }

        private void refresh(double beforeProbability, double afterProbability) {
            this.beforeProbability = beforeProbability;
            this.afterProbability = afterProbability;
            this.expiration = System.currentTimeMillis() + 1_000L;
        }

    }

    private static final class Reference {

        @Override
        public int hashCode() {
            return Objects.hash(this.hashCodes[0], this.hashCodes[1], this.sum);
        }

        private final int[] hashCodes;
        private final float sum;

        private Reference(float[] patterns) {
            this.hashCodes = getHashCodes(patterns);
            this.sum = patterns[0] + patterns[1] + patterns[2];
        }
    }

    private final Map<PlayerProfile, LinearPatternProfile> profiles;
    private final Map<Integer, List<Integer>> correlations;
    private final Map<Integer, Reference> references;
    private final Map<Integer, Short> overflow;
    private final int initialHash;
    private final Map<Integer, Cache> cache;

    protected LinearPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.initialHash = Objects.hash(pattern.key.hashCode(), System.currentTimeMillis());
        this.profiles = new HashMap<>(Pattern.individualDataBase, 1.0f);
        this.correlations = Collections.synchronizedMap(
                new HashMap<>(Pattern.individualDataBase, 1.0f)
        );
        this.references = new HashMap<>(Pattern.individualDataBase, 1.0f);
        this.overflow = new HashMap<>(Pattern.individualDataBase, 1.0f);
        this.cache = new HashMap<>(Pattern.individualDataBase, 1.0f);
    }

    @Override
    protected PatternProfile deleteProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.remove(profile);
        }
    }

    @Override
    protected boolean hasPatterns(int situation) {
        synchronized (this.correlations) {
            return this.correlations.containsKey(situation);
        }
    }

    @Override
    protected PatternProfile customLearn(PlayerProfile profile, int situation,
                                         float pattern, boolean read) {
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

        synchronized (patternProfile.correlations) {
            if (patternProfile.correlations.size() == 3) {
                patternProfile.correlations.remove(0);
                patternProfile.correlations.add(pattern);
            } else {
                patternProfile.correlations.add(pattern);
                return patternProfile;
            }
        }
        synchronized (this.correlations) {
            List<Integer> list = this.correlations.computeIfAbsent(
                    situation,
                    k -> new ArrayList<>(Pattern.individualDataBase)
            );
            Reference reference = new Reference(this.getPatterns(patternProfile));
            int referenceKey = HashUtils.extendInt(reference.hashCodes[0], reference.hashCodes[1]);
            this.references.putIfAbsent(referenceKey, new Reference(this.getPatterns(patternProfile)));

            if (list.size() == Pattern.individualDataLimit - 1) {
                short position = (short) (this.overflow.getOrDefault(situation, (short) -1) + 1);
                list.set(position, referenceKey);

                if (position == Pattern.individualDataLimit - 1) {
                    this.overflow.remove(situation);
                } else {
                    this.overflow.put(situation, position);
                }
            } else {
                list.add(referenceKey);
            }
        }
        return patternProfile;
    }

    @Override
    public Comparison customComparison(PlayerProfile profile, int situation) {
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.correlations.size() < 3) {
            return new Comparison();
        } else {
            Cache cache;
            float[] patterns = this.getPatterns(patternProfile);
            int hash = HashUtils.extendInt(
                    HashUtils.extendInt(this.initialHash, situation),
                    Float.hashCode(patterns[1])
            );

            synchronized (this.correlations) {
                cache = this.cache.get(hash);

                if (cache == null) {
                    Collection<Integer> collection = this.correlations.get(situation);

                    if (collection == null) {
                        cache = new Cache(0.0, 0.0);
                    } else {
                        int[] hashCodes = getHashCodes(patterns);
                        int beforeProbability = 0, afterProbability = 0;

                        for (int referenceKey : collection) {
                            Reference reference = this.references.get(referenceKey);

                            if (hashCodes[0] == reference.hashCodes[0]) {
                                beforeProbability++;
                            }
                            if (hashCodes[1] == reference.hashCodes[1]) {
                                afterProbability++;
                            }
                        }
                        double count = collection.size();
                        cache = new Cache(
                                beforeProbability / count,
                                afterProbability / count
                        );

                        if (this.cache.size() == Pattern.individualDataLimit - 1) {
                            Iterator<Integer> loop = this.cache.keySet().iterator();
                            loop.next();
                            loop.remove();
                        }
                        this.cache.put(hash, cache);
                    }
                } else if (cache.isExpired()) {
                    Collection<Integer> collection = this.correlations.get(situation);

                    if (collection == null) {
                        this.cache.remove(hash);
                        cache.refresh(0.0, 0.0);
                    } else {
                        int[] hashCodes = getHashCodes(patterns);
                        int beforeProbability = 0, afterProbability = 0;

                        for (int referenceKey : collection) {
                            Reference reference = this.references.get(referenceKey);

                            if (hashCodes[0] == reference.hashCodes[0]) {
                                beforeProbability++;
                            }
                            if (hashCodes[1] == reference.hashCodes[1]) {
                                afterProbability++;
                            }
                        }
                        double count = collection.size();
                        cache.refresh(
                                beforeProbability / count,
                                afterProbability / count
                        );
                    }
                }
            }
            return new Comparison(
                    this,
                    patternProfile,
                    situation,
                    new float[]{patterns[0], patterns[1], patterns[2]},
                    cache.beforeProbability
                            * cache.afterProbability
            );
        }
    }

    @Override
    protected void clearProfiles() {
        synchronized (this.profilesSynchronizer) {
            this.profiles.clear();
        }
    }

    @Override
    protected int getProfileCount() {
        return this.profiles.size();
    }

    // Separator

    private LinearPatternProfile getLocalProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> new LinearPatternProfile(profile)
            );
        }
    }

    private float[] getPatterns(LinearPatternProfile patternProfile) {
        synchronized (patternProfile.correlations) {
            return new float[]{
                    patternProfile.correlations.get(patternProfile.correlations.size() - 3),
                    patternProfile.correlations.get(patternProfile.correlations.size() - 2),
                    patternProfile.correlations.get(patternProfile.correlations.size() - 1)
            };
        }
    }

}
