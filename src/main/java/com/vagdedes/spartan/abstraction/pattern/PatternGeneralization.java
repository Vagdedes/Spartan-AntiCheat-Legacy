package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.math.implementation.MapMath;
import com.vagdedes.spartan.abstraction.math.implementation.SetMath;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.utils.math.AlgebraUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class PatternGeneralization {

    public static final class Comparison {

        public final double
                probability,
                contribution,
                significance,
                similarity,
                outcome;

        Comparison(PatternGeneralization patternGeneralization,
                   PatternProfile patternProfile,
                   int situation,
                   float pattern,
                   double probability) {
            this.probability = probability;
            this.contribution = patternGeneralization.contribution(situation, patternProfile, pattern);
            this.significance = patternGeneralization.significance(situation, patternProfile, pattern);
            this.similarity = patternGeneralization.similarity(situation, patternProfile, pattern);
            this.outcome = (this.probability + this.contribution + this.significance + this.similarity) / 4.0;
        }

    }

    public final Pattern parent;
    final short scissors;
    private final MapMath situationSignificance, patternSignificance;
    private final Map<Integer, SetMath> mass;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, short generalization) {
        this.parent = pattern;
        this.scissors = generalization;
        this.situationSignificance = new MapMath(Pattern.globalDataLimitBase, 1.0f);
        this.patternSignificance = new MapMath(Pattern.globalDataLimitBase, 1.0f);
        this.mass = Collections.synchronizedMap(new HashMap<>());
        this.profilesSynchronizer = new Object();
    }

    final void clear() {
        synchronized (this.profilesSynchronizer) {
            synchronized (this.mass) {
                synchronized (this.situationSignificance) {
                    synchronized (this.patternSignificance) {
                        this.clearProfiles();
                        this.mass.clear();
                        this.situationSignificance.clear();
                        this.patternSignificance.clear();
                    }
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
        if (this.situationSignificance.getParts() == Pattern.globalDataLimit - 1) {
            synchronized (this.situationSignificance) {
                this.situationSignificance.removeLeastSignificant();
                this.situationSignificance.add(pattern, patternProfile.hash);
            }
        } else {
            synchronized (this.situationSignificance) {
                this.situationSignificance.add(pattern, patternProfile.hash);
            }
        }
        synchronized (this.mass) {
            SetMath pie = this.mass.computeIfAbsent(
                    situation,
                    k -> new SetMath(Pattern.individualGlobalDataLimitBase, 1.0f)
            );

            if (pie.getParts() == Pattern.individualGlobalDataLimit - 1) {
                pie.removeLeastSignificant();
            }
            pie.add(pattern, patternProfile.hash);
        }
    }

    protected final void decreaseImportance(int situation, int profileHash, float pattern) {
        synchronized (this.situationSignificance) {
            this.situationSignificance.remove(pattern, profileHash);
        }
        synchronized (this.mass) {
            SetMath pie = this.mass.get(situation);

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

    final double significance(int situation, PatternProfile patternProfile, float pattern) {
        return 0.0; // todo
    }

    final double contribution(int situation, PatternProfile patternProfile, float pattern) {
        return 0.0; // todo
    }

    final double similarity(int situation, PatternProfile patternProfile, float pattern) {
        return 0.0; // todo
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
