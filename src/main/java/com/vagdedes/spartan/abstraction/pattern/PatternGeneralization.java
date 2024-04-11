package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.math.implementation.SetPie;
import com.vagdedes.spartan.abstraction.math.implementation.SetRank;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.*;

public abstract class PatternGeneralization {

    public static final class Comparison {

        public final boolean enoughData;
        public final double
                probability,
                mass,
                size,
                similarity,
                consistency;

        Comparison(PatternGeneralization patternGeneralization,
                   PatternProfile patternProfile,
                   int situation,
                   float pattern,
                   double probability,
                   int dataSize) {
            int dataProfiles = patternGeneralization.uniqueContribution(situation, patternProfile, pattern);
            this.enoughData = dataProfiles >= ResearchEngine.requiredProfiles;
            this.probability = probability;
            this.mass = (1.0 - AlgebraUtils.contribution(dataSize))
                    * (1.0 - AlgebraUtils.contribution(dataProfiles));
            this.size = patternGeneralization.size(pattern);
        }

    }

    public final Pattern parent;
    final short scissors;
    private final SetRank size;
    private final Map<Integer, SetPie> mass;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, short generalization) {
        this.parent = pattern;
        this.scissors = generalization;
        this.size = new SetRank(Pattern.globalDataLimitBase, 1.0f);
        this.mass = Collections.synchronizedMap(new HashMap<>());
        this.profilesSynchronizer = new Object();
    }

    final void clear() {
        synchronized (this.profilesSynchronizer) {
            synchronized (this.mass) {
                synchronized (this.size) {
                    this.clearProfiles();
                    this.mass.clear();
                    this.size.clear();
                }
            }
        }
    }

    // Separator

    final boolean learn(PlayerProfile profile,
                        long time,
                        int situation,
                        double pattern,
                        boolean store) {
        if (profile.isLegitimate()) {
            double original = pattern;

            if (this.scissors != 0) {
                pattern = AlgebraUtils.cut(pattern, this.scissors);
            }
            PatternProfile patternProfile = this.customLearn(profile, situation, (float) pattern);

            if (patternProfile != null) {
                this.increaseImportance(situation, patternProfile, (float) pattern);

                if (store) {
                    this.parent.setToFile(profile, time, situation, original);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            if (this.hasPatterns()) {
                Pattern.deleteFromFile(profile, false);
            }
            return false;
        }
    }

    // Separator

    private void increaseImportance(int situation, PatternProfile patternProfile, float pattern) {
        if (this.size.getParts() == Pattern.globalDataLimit - 1) {
            synchronized (this.size) {
                this.size.removeLeastSignificant();
                this.size.add(pattern, patternProfile.hash);
            }
        } else {
            synchronized (this.size) {
                this.size.add(pattern, patternProfile.hash);
            }
        }
        synchronized (this.mass) {
            SetPie pie = this.mass.computeIfAbsent(
                    situation,
                    k -> new SetPie(Pattern.individualGlobalDataLimitBase, 1.0f)
            );

            if (pie.getParts() == Pattern.individualGlobalDataLimit - 1) {
                pie.removeLeastSignificant();
            }
            pie.add(pattern, patternProfile.hash);
        }
    }

    protected final void decreaseImportance(int situation, int profileHash, float pattern) {
        synchronized (this.size) {
            this.size.remove(pattern, profileHash);
        }
        synchronized (this.mass) {
            SetPie pie = this.mass.get(situation);

            if (pie != null
                    && pie.remove(pattern, profileHash)
                    && pie.isEmpty()) {
                this.mass.remove(situation);
            }
        }
    }

    protected final void decreaseImportance(int situation, PatternProfile patternProfile, float pattern) {
        this.decreaseImportance(situation, patternProfile.hash, pattern);
    }

    final double size(float pattern) {
        synchronized (this.size) {
            return this.size.getPosition(pattern, Pattern.defaultPosition);
        }
    }

    final int uniqueContribution(int situation, PatternProfile patternProfile, float pattern) {
        synchronized (this.mass) {
            SetPie pie = this.mass.get(situation);

            if (pie == null) {
                return 0;
            } else {
                Set<Integer> set = pie.getSet(pattern);
                return set.contains(patternProfile.hash)
                        ? set.size() - 1
                        : set.size();
            }
        }
    }

    // Separator

    final void deleteProfile(PlayerProfile profile) {
        PatternProfile patternProfile;

        synchronized (this.profilesSynchronizer) {
            patternProfile = this.customDeleteProfile(profile);
        }
        if (patternProfile != null) {
            patternProfile.deleteCustomData();
        }
    }

    abstract protected PatternProfile customDeleteProfile(PlayerProfile profile);

    // Separator

    abstract protected boolean hasPatterns();

    abstract protected boolean hasPatterns(int situation);

    abstract protected PatternProfile customLearn(PlayerProfile profile, int situation, float pattern);

    public abstract Comparison comparison(PlayerProfile playerProfile, int situation);

    // Separator

    protected final void deletePattern(int situation, int profileHash, float pattern) {
        this.customDeletePattern(situation, pattern);
        this.decreaseImportance(situation, profileHash, pattern);
    }

    abstract protected void customDeletePattern(int situation, float pattern);

    // Separator

    abstract protected void clearProfiles();

    abstract protected Collection<? extends PatternProfile> getProfiles();

}
