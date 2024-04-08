package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.*;

public abstract class PatternGeneralization {

    final Pattern parent;
    final short scissors;
    private final ProbabilityPie significancePie;
    private final Set<PatternProfile> profilesToStore;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, short generalization) {
        this.parent = pattern;
        this.scissors = generalization;
        this.significancePie = new ProbabilityPie();
        this.profilesToStore = Collections.synchronizedSet(new HashSet<>());
        this.profilesSynchronizer = new Object();
    }

    void clear() {
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

    void storeProfiles(boolean local) {
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

    void queueStorage(PatternProfile profile) {
        synchronized (this.profilesToStore) {
            this.profilesToStore.add(profile);
        }
    }

    void dequeueStorage(PatternProfile profile) {
        synchronized (this.profilesToStore) {
            this.profilesToStore.remove(profile);
        }
    }

    // Separator

    boolean learn(PlayerProfile profile,
                  long time,
                  int situation,
                  double pattern,
                  boolean store) {
        if (profile.isLegitimate()) {
            if (this.scissors != 0) {
                pattern = AlgebraUtils.cut(pattern, this.scissors);
            }
            if (this.customLearn(profile, situation, (float) pattern)
                    && store) {
                this.parent.setToFile(profile, time, situation, pattern);
                return true;
            } else {
                return false;
            }
        } else {
            if (this.hasPatterns()) {
                Pattern.delete(profile);
            }
            return false;
        }
    }

    // Separator

    float averagePatternTime() {
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
        return !SpartanBukkit.debug && profiles < ResearchEngine.requiredProfiles
                ? -1.0f
                : time / (float) profiles;
    }

    // Separator

    protected void increaseSignificance(float pattern) {
        if (this.significancePie.getSlices() == Pattern.globalDataLimit) {
            synchronized (this.significancePie) {
                this.significancePie.removeLeastSignificant();
                this.significancePie.add(pattern);
            }
        } else {
            synchronized (this.significancePie) {
                this.significancePie.add(pattern);
            }
        }
    }

    protected void decreaseSignificance(float pattern) {
        synchronized (this.significancePie) {
            this.significancePie.remove(pattern);
        }
    }

    double significance(float value) {
        synchronized (this.significancePie) {
            return this.significancePie.getProbability(value);
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

    protected void deleteCustomLessons(int situation, double pattern) {
        this.decreaseSignificance((float) pattern);
        this.deleteCustomLessonsInternal(situation, (float) pattern);
    }

    abstract protected void deleteCustomLessonsInternal(int situation, float pattern);

    // Separator

    abstract protected void clearProfiles();

    abstract protected Collection<? extends PatternProfile> getProfiles();

}
