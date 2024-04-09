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

    final Pattern parent;
    final short scissors;
    private final HashPie significancePie;
    private final Set<PatternProfile> profilesToStore;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, short generalization) {
        this.parent = pattern;
        this.scissors = generalization;
        this.significancePie = new HashPie(Pattern.globalDataLimitBase, 1.0f);
        this.profilesToStore = Collections.synchronizedSet(new HashSet<>());
        this.profilesSynchronizer = new Object();
    }

    final void clear() {
        synchronized (this.profilesSynchronizer) {
            synchronized (this.significancePie) {
                synchronized (this.profilesToStore) {
                    this.clearProfiles();
                    this.significancePie.clear();
                    this.storeProfiles(true);
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
            if (this.customLearn(profile, situation, (float) pattern)
                    && store) {
                this.parent.setToFile(profile, time, situation, original);
                return true;
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

    protected final void increaseSignificance(int hash, float pattern) {
        if (this.significancePie.getSlices() == Pattern.globalDataLimit - 1) {
            synchronized (this.significancePie) {
                this.significancePie.removeLeastSignificant();
                this.significancePie.add(hash, pattern);
            }
        } else {
            synchronized (this.significancePie) {
                this.significancePie.add(hash, pattern);
            }
        }
    }

    protected final void decreaseSignificance(int hash, float pattern) {
        synchronized (this.significancePie) {
            this.significancePie.remove(hash, pattern);
        }
    }

    protected final double significance(float value) {
        synchronized (this.significancePie) {
            return this.significancePie.getRatio(value);
        }
    }

    // Separator

    PatternProfile removeProfile(PlayerProfile profile) {
        synchronized (this.profilesSynchronizer) {
            return this.removeProfileInternal(profile);
        }
    }

    abstract protected PatternProfile removeProfileInternal(PlayerProfile profile);

    // Separator

    abstract protected boolean hasPatterns();

    abstract protected boolean hasPatterns(int situation);

    abstract protected boolean customLearn(PlayerProfile profile, int situation, float pattern);

    public boolean compare(PlayerProfile profile, int situation) {
        return this.compareInternal(profile, System.currentTimeMillis(), situation);
    }

    protected abstract boolean compareInternal(PlayerProfile playerProfile, long time, int situation);

    // Separator

    protected final void deleteCustomLessons(int hash, int situation, float pattern) {
        this.decreaseSignificance(hash, pattern);
        this.deleteCustomLessonsInternal(situation, pattern);
    }

    abstract protected void deleteCustomLessonsInternal(int situation, float pattern);

    // Separator

    abstract protected void clearProfiles();

    abstract protected Collection<? extends PatternProfile> getProfiles();

    abstract public PatternProfile getProfile(PlayerProfile profile);

}
