package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.*;

public abstract class PatternGeneralization {

    final Pattern parent;
    final PatternGeneralizationInitiator initiator;
    private final ProbabilityPie significancePie;
    private final Set<PatternProfile> profilesToStore;
    protected final Object profilesSynchronizer;

    protected PatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        this.parent = pattern;
        this.initiator = initiator;
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

    void learn(PlayerProfile profile,
               long time,
               long situation,
               double pattern,
               boolean store) {
        if (profile.isLegitimate()) {
            if (this.initiator.scissors != 0) {
                pattern = AlgebraUtils.cut(pattern, this.initiator.scissors);
            }
            if (this.customLearn(profile, situation, pattern)
                    && store) {
                this.parent.setToFile(profile, time, situation, pattern);
            }
        }
    }

    // Separator

    double averagePatternTime() {
        double time = 0.0;
        int profiles = 0;

        synchronized (this.profilesSynchronizer) {
            for (PatternProfile profile : this.getProfiles()) {
                double average = profile.averagePatternTime();

                if (average != -1.0) {
                    time += average;
                    profiles++;
                }
            }
        }
        return !SpartanBukkit.debug && profiles < ResearchEngine.requiredProfiles
                ? -1.0
                : time / (double) profiles;
    }

    // Separator

    void addToSignificance(double pattern) {
        if (this.significancePie.getSlices() == this.initiator.cube) {
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

    double significance(double value) {
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

    abstract protected boolean hasPatterns(long situation);

    abstract protected boolean customLearn(PlayerProfile profile, long situation, double pattern);

    abstract protected void deleteCustomLessons(long situation, double pattern);

    // Separator

    abstract protected void clearProfiles();

    abstract protected Collection<? extends PatternProfile> getProfiles();

}
