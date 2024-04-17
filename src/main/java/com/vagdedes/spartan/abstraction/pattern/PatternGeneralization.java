package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.math.implementation.MapMath;
import com.vagdedes.spartan.abstraction.math.implementation.SetMath;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Bukkit;

public abstract class PatternGeneralization {

    public static final class Comparison {

        public final double
                probability,
                contribution,
                significance,
                outcome;

        Comparison() {
            this.probability = -1.0;
            this.contribution = -1.0;
            this.significance = -1.0;
            this.outcome = -1.0;
        }

        Comparison(PatternGeneralization patternGeneralization,
                   PatternProfile patternProfile,
                   int situation,
                   float pattern,
                   double probability) {
            this.probability = probability;
            this.contribution = patternGeneralization.contribution(situation, patternProfile);
            this.significance = patternGeneralization.significance(pattern);
            this.outcome = ((this.probability * this.contribution) + this.significance) / 2.0;
        }

        Comparison(PatternGeneralization patternGeneralization,
                   PatternProfile patternProfile,
                   int situation,
                   float[] patterns,
                   double probability) {
            this.probability = probability;
            this.contribution = patternGeneralization.contribution(situation, patternProfile);

            double significance = 0.0;

            for (float pattern : patterns) {
                significance += patternGeneralization.significance(pattern);
            }
            this.significance = significance / (double) patterns.length;
            this.outcome = ((this.probability * this.contribution) + this.significance) / 2.0;
        }

        public boolean isValid() {
            return this.outcome != -1.0;
        }

    }

    public final Pattern parent;
    final short scissors;
    private final SetMath significance;
    private final MapMath contribution;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, short generalization) {
        this.parent = pattern;
        this.scissors = generalization;
        this.significance = new SetMath(Pattern.globalDataLimitBase, 1.0f);
        this.contribution = new MapMath(Pattern.globalDataLimitBase, 1.0f);
        this.profilesSynchronizer = new Object();
    }

    final void clear() {
        synchronized (this.profilesSynchronizer) {
            synchronized (this.contribution) {
                synchronized (this.significance) {
                    this.clearProfiles();
                    this.contribution.clear();
                    this.significance.clear();
                }
            }
        }
    }

    // Separator

    final boolean learn(PlayerProfile profile, int situation, double pattern) {
        if (profile.isLegitimate()) { // todo fix
            if (this.scissors != 0) {
                pattern = AlgebraUtils.cut(pattern, this.scissors);
            }
           return this.customLearn(profile, situation, (float) pattern) != null;
        } else {
            return false;
        }
    }

    // Separator

    private void increaseImportance(int situation, PatternProfile patternProfile, float pattern) {
        if (this.significance.getParts() == Pattern.globalDataLimit - 1) {
            synchronized (this.significance) {
                this.significance.removeLeastSignificant();
                this.significance.add(pattern, patternProfile.hash);
            }
        } else {
            synchronized (this.significance) {
                this.significance.add(pattern, patternProfile.hash);
            }
        }
        if (this.contribution.getParts() == Pattern.globalDataLimit - 1) {
            synchronized (this.contribution) {
                this.contribution.removeLeastSignificant();
                this.contribution.add(situation, patternProfile.hash);
            }
        } else {
            synchronized (this.contribution) {
                this.contribution.add(situation, patternProfile.hash);
            }
        }
    }

    private void decreaseImportance(int situation, int profileHash) {
        synchronized (this.contribution) {
            this.contribution.remove(situation, profileHash);
        }
    }

    private double significance(float pattern) {
        int patternCount;

        synchronized (this.significance) {
            patternCount = this.significance.getCount(pattern);
        }
        return patternCount / (double) this.getProfileCount();
    }

    private double contribution(int situation, PatternProfile patternProfile) {
        synchronized (this.contribution) {
            return (1.0 - this.contribution.getPersonalContribution(situation, patternProfile.hash))
                    * (1.0 - this.contribution.getSpecificPersonalContribution(situation, patternProfile.hash));
        }
    }

    // Separator

    abstract protected PatternProfile deleteProfile(PlayerProfile profile);

    // Separator

    abstract protected boolean hasPatterns(int situation);

    abstract protected PatternProfile customLearn(PlayerProfile profile, int situation, float pattern);

    public final Comparison comparison(PlayerProfile playerProfile, int situation) {
        return this.parent.loaded
                ? this.customComparison(playerProfile, situation)
                : new Comparison();
    }

    protected abstract Comparison customComparison(PlayerProfile playerProfile, int situation);

    // Separator

    protected final void deletePattern(int situation, int profileHash, float pattern) {
        this.customDeletePattern(situation, pattern);
        this.decreaseImportance(situation, profileHash);
    }

    abstract protected void customDeletePattern(int situation, float pattern);

    // Separator

    abstract protected void clearProfiles();

    abstract protected int getProfileCount();

}
