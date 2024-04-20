package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.java.HashUtils;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    private static final double requiredRatio = 0.9;

    private static int[] getHashCodes(float[] patterns) {
        int nowHash = Float.hashCode(patterns[1]);
        return new int[]{
                HashUtils.extendInt(nowHash, Float.hashCode(patterns[0])),
                HashUtils.extendInt(nowHash, Float.hashCode(patterns[2]))
        };
    }

    // Separator

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

    // Separator

    private static final class Reference {

        private final float[] patterns;
        private final int[] hashCodes;
        private final double magnitude;

        @Override
        public int hashCode() {
            return Objects.hash(this.hashCodes[0], this.hashCodes[1]);
        }

        private Reference(float[] patterns) {
            double mangitude = 0.0;

            for (float pattern : patterns) {
                mangitude += pattern * pattern;
            }
            this.patterns = patterns;
            this.hashCodes = getHashCodes(patterns);
            this.magnitude = Math.sqrt(mangitude);
        }

    }

    // Separator

    private final Map<PlayerProfile, LinearPatternProfile> profiles;
    private final Map<Integer, List<Integer>> referenceKeys;
    private final Map<Integer, Integer> connections;
    private final Map<Integer, Reference> references;
    private final Map<Integer, Short> overflow;
    private final int initialHash;
    private final Map<Integer, Cache> cache;

    protected LinearPatternGeneralization(Pattern pattern, short generalization) {
        super(pattern, generalization);
        this.initialHash = Objects.hash(pattern.key.hashCode(), System.currentTimeMillis());
        this.profiles = new HashMap<>(Pattern.individualDataBase, 1.0f);
        this.referenceKeys = Collections.synchronizedMap(
                new HashMap<>(Pattern.individualDataBase, 1.0f)
        );
        this.connections = Collections.synchronizedMap(
                new HashMap<>(Pattern.individualDataBase, 1.0f)
        );
        this.references = Collections.synchronizedMap(
                new HashMap<>(Pattern.individualDataBase, 1.0f)
        );
        this.overflow = new HashMap<>(Pattern.individualDataBase, 1.0f);
        this.cache = new HashMap<>(Pattern.individualDataBase, 1.0f);
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
        synchronized (this.referenceKeys) {
            List<Integer> list = this.referenceKeys.computeIfAbsent(
                    situation,
                    k -> new ArrayList<>(Pattern.individualDataBase)
            );
            Reference reference = new Reference(this.getPatterns(patternProfile));
            int referenceKey = HashUtils.extendInt(reference.hashCodes[0], reference.hashCodes[1]);

            synchronized (this.references) {
                this.references.putIfAbsent(referenceKey, reference);
            }

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

            synchronized (this.referenceKeys) {
                cache = this.cache.get(hash);

                if (cache == null) {
                    Collection<Integer> collection = this.getReferences(situation);

                    if (collection == null) {
                        cache = new Cache(0.0, 0.0);
                    } else {
                        int[] hashCodes = getHashCodes(patterns);
                        int beforeProbability = 0, afterProbability = 0;

                        for (int referenceKey : collection) {
                            Reference reference;

                            synchronized (this.references) {
                                reference = this.references.get(referenceKey);
                            }

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
                    Collection<Integer> collection = this.getReferences(situation);

                    if (collection == null) {
                        this.cache.remove(hash);
                        cache.refresh(0.0, 0.0);
                    } else {
                        int[] hashCodes = getHashCodes(patterns);
                        int beforeProbability = 0, afterProbability = 0;

                        for (int referenceKey : collection) {
                            Reference reference;

                            synchronized (this.references) {
                                reference = this.references.get(referenceKey);
                            }

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

    @Override
    protected PatternProfile deleteProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.profiles.remove(profile);
        }
    }

    @Override
    protected boolean hasPatterns(int situation) {
        synchronized (this.referenceKeys) {
            return this.referenceKeys.containsKey(situation);
        }
    }

    @Override
    void loadCompleted() {
        if (!this.referenceKeys.isEmpty()) {
            Collection<Integer> known = new ArrayList<>(Pattern.individualDataBase),
                    unknown = new ArrayList<>(Pattern.individualDataBase);

            for (int situation : this.referenceKeys.keySet()) {
                if (this.contribution(situation) >= requiredRatio) {
                    known.add(situation);
                } else {
                    unknown.add(situation);
                }
            }

            if (!known.isEmpty() && !unknown.isEmpty()) {
                for (int knownSituation : known) {
                    for (int unknownSituation : unknown) {
                        this.connect(knownSituation, unknownSituation, false);
                    }
                }
            }
        }
    }

    private void connect(int knownSituation, int unknownSituation, boolean live) {
        if (live) {
            if (this.contribution(knownSituation) >= requiredRatio) {
                if (this.contribution(unknownSituation) >= requiredRatio) {
                    return;
                }
            } else if (this.contribution(unknownSituation) < requiredRatio) {
                return;
            } else {
                int situationCopy = knownSituation;
                knownSituation = unknownSituation;
                unknownSituation = situationCopy;
            }
        }
        double similarity = 0.0;
        List<Integer> knownSituationCorrelations = this.referenceKeys.get(knownSituation),
                unknownSituationCorrelations = this.referenceKeys.get(unknownSituation);

        if (knownSituationCorrelations.size() == unknownSituationCorrelations.size()) {
            for (int i = 0; i < knownSituationCorrelations.size(); i++) {
                Reference knownReference, unknownReference;

                synchronized (this.references) {
                    knownReference = this.references.get(knownSituationCorrelations.get(i));
                    unknownReference = this.references.get(unknownSituationCorrelations.get(i));
                }
                double dotProduct = 0.0;

                for (int j = 0; j < knownReference.patterns.length; j++) {
                    dotProduct += knownReference.patterns[j] * unknownReference.patterns[j];
                }
                similarity += dotProduct / (knownReference.magnitude * unknownReference.magnitude);
            }
            similarity /= knownSituationCorrelations.size();
        } else {
            List<Integer> bigList, smallList;

            if (knownSituationCorrelations.size() > unknownSituationCorrelations.size()) {
                bigList = knownSituationCorrelations;
                smallList = unknownSituationCorrelations;
            } else {
                bigList = unknownSituationCorrelations;
                smallList = knownSituationCorrelations;
            }
            int rest = bigList.size() % smallList.size();

            for (int i = 0; i < (bigList.size() / smallList.size()); i++) {
                for (int j = 0; j < smallList.size(); j++) {
                    Reference knownReference, unknownReference;

                    synchronized (this.references) {
                        knownReference = this.references.get(bigList.get(i * smallList.size() + j));
                        unknownReference = this.references.get(smallList.get(j));
                    }
                    double dotProduct = 0.0;

                    for (int k = 0; k < knownReference.patterns.length; k++) {
                        dotProduct += knownReference.patterns[k] * unknownReference.patterns[k];
                    }
                    similarity += dotProduct / (knownReference.magnitude * unknownReference.magnitude);
                }
            }
            if (rest > 0) {
                for (int i = 0; i < rest; i++) {
                    Reference knownReference, unknownReference;

                    synchronized (this.references) {
                        knownReference = this.references.get(bigList.get(bigList.size() - rest + i));
                        unknownReference = this.references.get(smallList.get(i));
                    }
                    double dotProduct = 0.0;

                    for (int j = 0; j < knownReference.patterns.length; j++) {
                        dotProduct += knownReference.patterns[j] * unknownReference.patterns[j];
                    }
                    similarity += dotProduct / (knownReference.magnitude * unknownReference.magnitude);
                }
            }
            similarity /= bigList.size();
        }

        if (similarity >= 0.9) {
            Integer connected = this.connections.get(unknownSituation);

            if (connected == null) {
                this.connections.put(unknownSituation, knownSituation);
            } else if ((!live || connected != knownSituation)
                    && this.contribution(connected) < this.contribution(knownSituation)) {
                this.connections.put(unknownSituation, knownSituation);
            }
        }
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

    private Collection<Integer> getReferences(int situation) {
        Integer connected;

        synchronized (this.connections) {
            connected = this.connections.get(situation);
        }
        if (connected != null) {
            synchronized (this.referenceKeys) {
                return this.referenceKeys.get(connected);
            }
        } else {
            synchronized (this.referenceKeys) {
                return this.referenceKeys.get(situation);
            }
        }
    }

}
