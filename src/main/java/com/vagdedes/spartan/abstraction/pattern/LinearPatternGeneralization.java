package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.HashUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.java.OverflowList;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

    private static final int requiredPatterns = 3;
    private static final double
            requiredContribution = 0.9,
            requiredSimilarity = 0.9;

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

        private Cache() {
            this.refresh();
        }

        private Cache(double beforeProbability, double afterProbability, int dataSize) {
            this.refresh(beforeProbability, afterProbability, dataSize);
        }

        private boolean isExpired() {
            return this.expiration < System.currentTimeMillis();
        }

        private void refresh(double beforeProbability, double afterProbability, int dataSize) {
            this.beforeProbability = beforeProbability / (double) dataSize;
            this.afterProbability = afterProbability / (double) dataSize;
            this.expiration = System.currentTimeMillis()
                    + AlgebraUtils.integerRound((dataSize / (double) Pattern.individualDataLimit) * 1_000);
        }

        private void refresh() {
            this.beforeProbability = 0.0;
            this.afterProbability = 0.0;
            this.expiration = 0L;
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
    private final Map<Integer, OverflowList<Integer>> referenceKeys;
    private final Map<Integer, Integer> connections;
    private final Map<Integer, Reference> references;
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
        this.cache = new HashMap<>(Pattern.individualDataBase, 1.0f);
    }

    @Override
    protected PatternProfile customLearn(PlayerProfile profile, int[] situation,
                                         float pattern, boolean read) {
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

        synchronized (patternProfile.patterns) {
            if (patternProfile.patterns.size() == requiredPatterns) {
                patternProfile.patterns.remove(0);
                patternProfile.patterns.add(pattern);
            } else {
                patternProfile.patterns.add(pattern);
                return null;
            }
        }
        if (!read && !this.canContribute(situation, patternProfile)) {
            return null;
        }
        synchronized (this.referenceKeys) {
            Reference reference = new Reference(this.getPatterns(patternProfile));
            int referenceKey = HashUtils.extendInt(reference.hashCodes[0], reference.hashCodes[1]);

            synchronized (this.references) {
                this.references.putIfAbsent(referenceKey, reference);
            }
            this.referenceKeys.computeIfAbsent(
                    situation[0],
                    k -> new OverflowList<>(Pattern.individualDataBase, Pattern.individualDataLimit)
            ).add(referenceKey);
        }
        if (!read) {
            if (!patternProfile.situations.isEmpty()) {
                boolean connected;

                synchronized (this.connections) {
                    connected = this.connections.containsKey(situation[0]);
                }

                if (connected) {
                    if (this.contribution(situation) >= requiredContribution) {
                        synchronized (this.connections) {
                            this.connections.remove(situation[0]);
                        }
                    }
                } else {
                    for (int[] previousSituation : patternProfile.situations) {
                        if (this.connect(previousSituation, situation)) {
                            break;
                        }
                    }
                }
            }
            patternProfile.situations.add(situation);
        }
        return patternProfile;
    }

    @Override
    public Comparison customComparison(PlayerProfile profile, int[] situation) {
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.patterns.size() < requiredPatterns) {
            return new Comparison();
        } else {
            situation = this.getActualSituation(situation);
            Cache cache;
            float[] patterns = this.getPatterns(patternProfile);
            int hash = HashUtils.extendInt(
                    HashUtils.extendInt(this.initialHash, situation[0]),
                    Float.hashCode(patterns[1])
            );

            synchronized (this.referenceKeys) {
                cache = this.cache.get(hash);

                if (cache == null) {
                    Collection<Integer> collection = this.referenceKeys.get(situation[0]);

                    if (collection == null) {
                        cache = new Cache();
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
                        cache = new Cache(
                                beforeProbability,
                                afterProbability,
                                collection.size()
                        );

                        if (this.cache.size() == Pattern.individualDataLimit - 1) {
                            Iterator<Integer> loop = this.cache.keySet().iterator();
                            loop.next();
                            loop.remove();
                        }
                        this.cache.put(hash, cache);
                    }
                } else if (cache.isExpired()) {
                    Collection<Integer> collection = this.referenceKeys.get(situation[0]);

                    if (collection == null) {
                        this.cache.remove(hash);
                        cache.refresh();
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
                        cache.refresh(
                                beforeProbability,
                                afterProbability,
                                collection.size()
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
    protected boolean hasPatterns(int[] situation) {
        synchronized (this.referenceKeys) {
            return this.referenceKeys.containsKey(situation[0]);
        }
    }

    @Override
    void loadStarting() {
        if (this.parent.key.endsWith("/vertical")) {
            Collection<Double>
                    heights = GroundUtils.getHeights(),
                    jumps = PlayerUtils.getJumpMotions();

            for (int jump = 0; jump < 2; jump++) {
                boolean jumping = jump == 1;

                for (int fall = 0; fall < 2; fall++) {
                    boolean falling = fall == 1;

                    for (double height : heights) {
                        int airTicks = 0;

                        for (PlayerProfile profile : Pattern.artificialProfiles) {
                            LinearPatternProfile patternProfile = this.getLocalProfile(profile);
                            patternProfile.patterns.clear();

                            for (int i = 0; i < requiredPatterns; i++) {
                                patternProfile.patterns.add(0.0f);
                            }
                        }

                        if (!jumping && !falling) {
                            for (double biggerHeight : heights) {
                                if (biggerHeight > height) {
                                    double step = biggerHeight - height;

                                    if (step <= GroundUtils.maxPlayerStep) {
                                        this.preTrain(
                                                airTicks,
                                                biggerHeight,
                                                height,
                                                step
                                        );
                                    }
                                }
                            }
                        } else {
                            double previousHeight;

                            if (jumping) {
                                for (double jumpMotion : jumps) {
                                    previousHeight = height;
                                    height += jumpMotion;
                                    airTicks++;
                                    this.preTrain(
                                            airTicks,
                                            height,
                                            previousHeight,
                                            jumpMotion
                                    );
                                }
                            }
                            if (falling) {
                                for (double fallMotion : PlayerUtils.calculateFallMotions(
                                        jumping
                                                ? PlayerUtils.getJumpMotion(PlayerUtils.getJumpTicks() - 1)
                                                : 0.0,
                                        MovementProcessing.motionLearningDecimals
                                )) {
                                    previousHeight = height;
                                    height += fallMotion;
                                    airTicks++;
                                    this.preTrain(
                                            airTicks,
                                            height,
                                            previousHeight,
                                            fallMotion
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void preTrain(int airTicks,
                          double height,
                          double previousHeight,
                          double pattern) {
        boolean ground = airTicks == 0;

        for (int sneak = 0; sneak < 2; sneak++) {
            this.selfLearn(
                    Pattern.hashSituation(new int[]{
                            MovementProcessing.artificialPlayerHash(
                                    sneak == 0,
                                    ground
                            ),
                            HashUtils.hashEnvironment(
                                    airTicks,
                                    AlgebraUtils.removeBase(height),
                                    AlgebraUtils.removeBase(previousHeight)
                            ),
                            MovementProcessing.defaultHashTrackers()
                    }),
                    pattern
            );
        }
    }

    @Override
    void loadCompleted() {

    }

    private boolean connect(int[] knownSituation, int[] unknownSituation) {
        if (knownSituation[0] != unknownSituation[0]) {
            if (this.contribution(knownSituation) >= requiredContribution) {
                if (this.contribution(unknownSituation) >= requiredContribution) {
                    return false;
                }
            } else if (this.contribution(unknownSituation) < requiredContribution) {
                return false;
            } else {
                int[] situationCopy = knownSituation;
                knownSituation = unknownSituation;
                unknownSituation = situationCopy;
            }

            synchronized (this.connections) {
                if (this.connections.containsKey(unknownSituation[0])) {
                    return true;
                }
            }
        } else {
            return false;
        }
        double similarity = 0.0;
        List<Integer> knownSituationCorrelations = this.referenceKeys.get(knownSituation[0]),
                unknownSituationCorrelations = this.referenceKeys.get(unknownSituation[0]);

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

        if (similarity >= requiredSimilarity) {
            this.connections.put(unknownSituation[0], knownSituation[0]);
            return true;
        } else {
            return false;
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
        synchronized (patternProfile.patterns) {
            return new float[]{
                    patternProfile.patterns.get(patternProfile.patterns.size() - requiredPatterns),
                    patternProfile.patterns.get(patternProfile.patterns.size() - 2),
                    patternProfile.patterns.get(patternProfile.patterns.size() - 1)
            };
        }
    }

    private int[] getActualSituation(int[] situation) {
        Integer connected;

        synchronized (this.connections) {
            connected = this.connections.get(situation[0]);
        }
        return connected == null ? situation : new int[]{connected};
    }

}
