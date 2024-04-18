package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.java.HashUtils;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

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

    private final Map<PlayerProfile, LinearPatternProfile> profiles;
    private final Map<Integer, Map<Integer, List<int[]>>> correlations;
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
    protected PatternProfile deleteProfile(PlayerProfile profile) {
        LinearPatternProfile patternProfile;

        synchronized (this.profilesSynchronizer) {
            patternProfile = this.profiles.remove(profile);
        }

        if (patternProfile != null) {
            synchronized (this.correlations) {
                Iterator<Map<Integer, List<int[]>>> iterator = this.correlations.values().iterator();

                while (iterator.hasNext()) {
                    Map<Integer, List<int[]>> map = iterator.next();

                    if (map.remove(patternProfile.hash) != null
                            && map.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
        }
        return patternProfile;
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
            Map<Integer, List<int[]>> map = this.correlations.get(situation);

            if (map == null) {
                map = new HashMap<>(Pattern.individualGlobalDataLimitBase, 1.0f);
                this.correlations.put(situation, map);
            } else if (map.size() == Pattern.individualGlobalDataLimit - 1) {
                int min = Integer.MAX_VALUE;
                Integer key = null;
                Iterator<Map.Entry<Integer, List<int[]>>> iterator = map.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Integer, List<int[]>> entry = iterator.next();
                    int count = 0;

                    for (List<int[]> list : map.values()) {
                        count += list.size();
                    }
                    if (count < min) {
                        min = count;

                        if (min == 1) {
                            iterator.remove();
                            break;
                        } else {
                            key = entry.getKey();
                        }
                    }
                }
                if (key != null) {
                    map.remove(key);
                }
            }
            List<int[]> list = map.computeIfAbsent(
                    patternProfile.hash,
                    k -> new ArrayList<>(Pattern.individualGlobalDataLimit)
            );
            if (list.size() == 100) {
                list.remove(0);
            }
            list.add(this.getHashCodes(this.getPatterns(patternProfile)));
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
                    Map<Integer, List<int[]>> map = this.correlations.get(situation);

                    if (map == null) {
                        cache = new Cache(0.0, 0.0);
                    } else {
                        int[] hashCodes = this.getHashCodes(patterns);
                        int beforeProbability = 0, afterProbability = 0;
                        double count = 0;

                        for (List<int[]> list : map.values()) {
                            for (int[] array : list) {
                                if (hashCodes[0] == array[0]) {
                                    beforeProbability++;
                                }
                                if (hashCodes[1] == array[1]) {
                                    afterProbability++;
                                }
                                count++;
                            }
                        }
                        cache = new Cache(
                                beforeProbability / count,
                                afterProbability / count
                        );

                        if (this.cache.size() == Pattern.individualGlobalDataLimit - 1) {
                            Iterator<Integer> loop = this.cache.keySet().iterator();
                            loop.next();
                            loop.remove();
                        }
                        this.cache.put(hash, cache);
                    }
                } else if (cache.isExpired()) {
                    Map<Integer, List<int[]>> map = this.correlations.get(situation);

                    if (map == null) {
                        this.cache.remove(hash);
                        cache.refresh(0.0, 0.0);
                    } else {
                        int[] hashCodes = this.getHashCodes(patterns);
                        int beforeProbability = 0, afterProbability = 0;
                        double count = 0;

                        for (List<int[]> list : map.values()) {
                            for (int[] array : list) {
                                if (hashCodes[0] == array[0]) {
                                    beforeProbability++;
                                }
                                if (hashCodes[1] == array[1]) {
                                    afterProbability++;
                                }
                                count++;
                            }
                        }
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
        this.profiles.clear();
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

    private int[] getHashCodes(float[] patterns) {
        int nowHash = Float.hashCode(patterns[1]);
        return new int[]{
                HashUtils.extendInt(nowHash, Float.hashCode(patterns[0])),
                HashUtils.extendInt(nowHash, Float.hashCode(patterns[2]))
        };
    }

}
