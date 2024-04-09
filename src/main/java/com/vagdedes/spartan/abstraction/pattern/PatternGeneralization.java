package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.HashPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.*;

public abstract class PatternGeneralization {

    public final Pattern parent;
    final short scissors;
    private final HashPie significance;
    private final Map<Integer, HashPie> uniqueContribution;
    private final Set<PatternProfile> profilesToStore;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, short generalization) {
        this.parent = pattern;
        this.scissors = generalization;
        this.significance = new HashPie(Pattern.globalDataLimitBase, 1.0f);
        this.uniqueContribution = Collections.synchronizedMap(new HashMap<>());
        this.profilesToStore = Collections.synchronizedSet(new HashSet<>());
        this.profilesSynchronizer = new Object();
    }

    final void clear() {
        synchronized (this.profilesSynchronizer) {
            synchronized (this.significance) {
                synchronized (this.profilesToStore) {
                    synchronized (this.uniqueContribution) {
                        this.clearProfiles();
                        this.significance.clear();
                        this.uniqueContribution.clear();
                        this.storeProfiles(true);
                    }
                }
            }
        }
    }

    // Separator

    final void storeProfiles(boolean local) {
        if (local) {
            if (!this.profilesToStore.isEmpty()) {
                Iterator<PatternProfile> iterator = this.profilesToStore.iterator();

                while (iterator.hasNext()) {
                    iterator.next().store();
                    iterator.remove();
                }
            }
        } else if (!this.profilesToStore.isEmpty()) {
            synchronized (this.profilesToStore) {
                Iterator<PatternProfile> iterator = this.profilesToStore.iterator();

                while (iterator.hasNext()) {
                    iterator.next().store();
                    iterator.remove();
                }
            }
        }
    }

    final void queueStorage(PatternProfile profile) {
        synchronized (this.profilesToStore) {
            this.profilesToStore.add(profile);
        }
    }

    final void dequeueStorage(PatternProfile profile) {
        synchronized (this.profilesToStore) {
            this.profilesToStore.remove(profile);
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

    public final double rankTrustFactor(SpartanPlayer player, double trustFactor) {
        if (trustFactor == -1.0) {
            return 1.0;
        } else {
            ProbabilityRank rank = new ProbabilityRank();
            Integer divisor = SpartanBukkit.testMode
                    ? DetectionNotifications.getDivisor(player, false)
                    : null;

            if (divisor == null || divisor != Pattern.testingNotificationDivisor) {
                synchronized (this.profilesSynchronizer) {
                    for (PatternProfile profile : this.getProfiles()) {
                        double other = profile.trustFactor(null);

                        if (other != -1.0) {
                            rank.add(other);
                        }
                    }
                }
            } else {
                rank.addMultiple(this.getProfile(player.getProfile()).trustFactors());
            }
            return rank.getParts() < ResearchEngine.requiredProfiles
                    ? 1.0
                    : rank.getPosition(trustFactor);
        }
    }

    // Separator

    final float averagePatternTime() {
        float time = 0.0f;
        int profiles = 0;

        synchronized (this.profilesSynchronizer) {
            for (PatternProfile profile : this.getProfiles()) {
                float average = profile.averagePatternTime();

                if (average != -1.0f) {
                    time += average;
                    profiles++;
                }
            }
        }
        return profiles < ResearchEngine.requiredProfiles
                ? -1.0f
                : time / (float) profiles;
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
                    k -> new HashPie(Pattern.globalDataLimitBase, 1.0f)
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

            if (pie != null) {
                pie.remove(pattern, patternProfile.hash);

                if (pie.isEmpty()) {
                    this.uniqueContribution.remove(situation);
                }
            }
        }
    }

    protected final double significance(float pattern) {
        synchronized (this.significance) {
            return this.significance.getRatio(pattern);
        }
    }

    protected final double uniqueContribution(int situation, float pattern) {
        synchronized (this.uniqueContribution) {
            HashPie pie = this.uniqueContribution.get(situation); // todo
            return pie == null ? 1.0 : AlgebraUtils.contribution(pie.getRatio(pattern));
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
