package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.pattern.implementation.MovementPatterns;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.HashUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.java.OverflowList;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public class LinearPatternGeneralization extends PatternGeneralization {

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
    protected PatternProfile customLearn(PlayerProfile profile, int situation,
                                         float pattern, boolean read) {
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

        synchronized (patternProfile.patterns) {
            if (patternProfile.patterns.size() == 3) {
                patternProfile.patterns.remove(0);
                patternProfile.patterns.add(pattern);
            } else {
                patternProfile.patterns.add(pattern);
                return null;
            }
        }
        if (!read && this.contributionAgainstRest(situation, patternProfile) >= 0.5) {
            return null;
        } else {
            synchronized (this.referenceKeys) {
                Reference reference = new Reference(this.getPatterns(patternProfile));
                int referenceKey = HashUtils.extendInt(reference.hashCodes[0], reference.hashCodes[1]);

                synchronized (this.references) {
                    this.references.putIfAbsent(referenceKey, reference);
                }
                this.referenceKeys.computeIfAbsent(
                        situation,
                        k -> new OverflowList<>(Pattern.individualDataBase, Pattern.individualDataLimit)
                ).add(referenceKey);
            }
            if (!read) {
                if (!patternProfile.situations.isEmpty()) {
                    boolean connected;

                    synchronized (this.connections) {
                        connected = this.connections.containsKey(situation);
                    }

                    if (connected) {
                        if (this.contribution(situation) >= requiredContribution) {
                            synchronized (this.connections) {
                                this.connections.remove(situation);
                            }
                        }
                    } else {
                        for (int previousSituation : patternProfile.situations) {
                            if (this.connect(previousSituation, situation)) {
                                break;
                            }
                        }
                    }
                }
                patternProfile.situations.add(situation);
            }
        }
        return patternProfile;
    }

    @Override
    public Comparison customComparison(PlayerProfile profile, int situation) {
        LinearPatternProfile patternProfile = this.getLocalProfile(profile);

        if (patternProfile.patterns.size() < 3) {
            return new Comparison();
        } else {
            situation = this.getActualSituation(situation);
            Cache cache;
            float[] patterns = this.getPatterns(patternProfile);
            int hash = HashUtils.extendInt(
                    HashUtils.extendInt(this.initialHash, situation),
                    Float.hashCode(patterns[1])
            );

            synchronized (this.referenceKeys) {
                cache = this.cache.get(hash);

                if (cache == null) {
                    Collection<Integer> collection = this.referenceKeys.get(situation);

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
                    Collection<Integer> collection = this.referenceKeys.get(situation);

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
    protected boolean hasPatterns(int situation) {
        synchronized (this.referenceKeys) {
            return this.referenceKeys.containsKey(situation);
        }
    }

    @Override
    void loadStarting() {
        if (this.parent.key.equals(MovementPatterns.verticalKey)) {
            for (double height : GroundUtils.getHeights()) {
                for (int ping = 0; ping < (TPS.tickTimeInteger * 4); ping += TPS.tickTimeInteger) {
                    for (int jump = 0; jump < 2; jump++) {
                        for (int sneak = 0; sneak < 2; sneak++) {
                            boolean sneaking = sneak == 1;

                            for (int fall = 0; fall < 2; fall++) {
                                int airTicks = 0;
                                double previousHeight;

                                if (jump == 1) {
                                    for (double jumpMotion : PlayerUtils.getJumpMotions()) {
                                        previousHeight = height;
                                        height += jumpMotion;
                                        this.preTrain(
                                                sneaking,
                                                airTicks,
                                                ping,
                                                height,
                                                previousHeight,
                                                jumpMotion
                                        );
                                        airTicks++; // After
                                    }
                                }
                                if (fall == 1) {
                                    for (double fallMotion : PlayerUtils.getFallMotions()) {
                                        previousHeight = height;
                                        height -= fallMotion;
                                        airTicks++; // Before
                                        this.preTrain(
                                                sneaking,
                                                airTicks,
                                                ping,
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
    }

    private void preTrain(boolean sneaking,
                          int airTicks,
                          int ping,
                          double height,
                          double previousHeight,
                          double pattern) {
        this.selfLearn(
                MovementProcessing.hashTotal(
                        MovementProcessing.artificialPlayerHash(
                                sneaking,
                                airTicks == 0,
                                ping
                        ),
                        HashUtils.hashEnvironment(
                                airTicks,
                                AlgebraUtils.removeBase(height),
                                AlgebraUtils.removeBase(previousHeight)
                        ),
                        1
                ),
                pattern
        );
    }

    @Override
    void loadCompleted() {

    }

    private boolean connect(int knownSituation, int unknownSituation) {
        if (knownSituation != unknownSituation) {
            if (this.contribution(knownSituation) >= requiredContribution) {
                if (this.contribution(unknownSituation) >= requiredContribution) {
                    return false;
                }
            } else if (this.contribution(unknownSituation) < requiredContribution) {
                return false;
            } else {
                int situationCopy = knownSituation;
                knownSituation = unknownSituation;
                unknownSituation = situationCopy;
            }

            synchronized (this.connections) {
                if (this.connections.containsKey(unknownSituation)) {
                    return true;
                }
            }
        } else {
            return false;
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

        if (similarity >= requiredSimilarity) {
            this.connections.put(unknownSituation, knownSituation);
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
                    patternProfile.patterns.get(patternProfile.patterns.size() - 3),
                    patternProfile.patterns.get(patternProfile.patterns.size() - 2),
                    patternProfile.patterns.get(patternProfile.patterns.size() - 1)
            };
        }
    }

    private int getActualSituation(int situation) {
        Integer connected;

        synchronized (this.connections) {
            connected = this.connections.get(situation);
        }
        return connected == null ? situation : connected;
    }

}
