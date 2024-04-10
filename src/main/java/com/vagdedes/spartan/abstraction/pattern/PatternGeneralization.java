package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.HashPie;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class PatternGeneralization {

    public final Pattern parent;
    final short scissors;
    private final HashPie significance;
    private final Map<Integer, HashPie> uniqueContribution;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, short generalization) {
        this.parent = pattern;
        this.scissors = generalization;
        this.significance = new HashPie(Pattern.globalDataLimitBase, 1.0f);
        this.uniqueContribution = Collections.synchronizedMap(new HashMap<>());
        this.profilesSynchronizer = new Object();
    }

    final void clear() {
        synchronized (this.profilesSynchronizer) {
            synchronized (this.significance) {
                synchronized (this.uniqueContribution) {
                    this.clearProfiles();
                    this.significance.clear();
                    this.uniqueContribution.clear();
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
                Pattern.deleteFromFile(profile);
            }
            return false;
        }
    }

    // Separator

    private void increaseImportance(int situation, PatternProfile patternProfile, float pattern) {
        if (this.significance.getSlices() == Pattern.globalDataLimit - 1) {
            synchronized (this.significance) {
                this.significance.removeLeastSignificant();
                this.significance.add(pattern, patternProfile.hash);
            }
        } else {
            synchronized (this.significance) {
                this.significance.add(pattern, patternProfile.hash);
            }
        }
        synchronized (this.uniqueContribution) {
            HashPie pie = this.uniqueContribution.computeIfAbsent(
                    situation,
                    k -> new HashPie(Pattern.individualGlobalDataLimitBase, 1.0f)
            );

            if (pie.getSlices() == Pattern.individualGlobalDataLimit - 1) {
                pie.removeLeastSignificant();
            }
            pie.add(pattern, patternProfile.hash);
        }
    }

    protected final void decreaseImportance(int situation, PatternProfile patternProfile, float pattern) {
        synchronized (this.significance) {
            this.significance.remove(pattern, patternProfile.hash);
        }
        synchronized (this.uniqueContribution) {
            HashPie pie = this.uniqueContribution.get(situation);

            if (pie != null
                    && pie.remove(pattern, patternProfile.hash)
                    && pie.isEmpty()) {
                this.uniqueContribution.remove(situation);
            }
        }
    }

    final double significance(float pattern) {
        synchronized (this.significance) {
            return this.significance.getRatio(pattern, Pattern.defaultRatio);
        }
    }

    final double uniqueContribution(int situation, float pattern) {
        synchronized (this.uniqueContribution) {
            HashPie pie = this.uniqueContribution.get(situation);
            return pie == null
                    ? Pattern.defaultRatio
                    : AlgebraUtils.contribution(pie.getRatio(pattern, Pattern.defaultRatio));
        }
    }

    // Separator

    final PatternProfile deleteProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.customDeleteProfile(profile);
        }
    }

    abstract protected PatternProfile customDeleteProfile(PlayerProfile profile);

    // Separator

    abstract protected boolean hasPatterns();

    abstract protected boolean hasPatterns(int situation);

    abstract protected PatternProfile customLearn(PlayerProfile profile, int situation, float pattern);

    public boolean compare(PlayerProfile profile, int situation) {
        return this.customCompare(profile, System.currentTimeMillis(), situation);
    }

    protected abstract boolean customCompare(PlayerProfile playerProfile, long time, int situation);

    // Separator

    protected final void deletePattern(int situation, PatternProfile patternProfile, float pattern) {
        this.customDeletePattern(situation, pattern);
        this.decreaseImportance(situation, patternProfile, pattern);
    }

    abstract protected void customDeletePattern(int situation, float pattern);

    // Separator

    abstract protected void clearProfiles();

    abstract protected Collection<? extends PatternProfile> getProfiles();

    abstract public PatternProfile getProfile(PlayerProfile profile);

}
