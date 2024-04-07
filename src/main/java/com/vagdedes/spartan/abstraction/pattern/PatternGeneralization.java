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
    final ProbabilityPie significancePie;
    final Map<PlayerProfile, PatternProfile> profiles;
    final Set<PatternProfile> profilesToStore;

    protected PatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        this.parent = pattern;
        this.initiator = initiator;
        this.significancePie = new ProbabilityPie();
        this.profiles = Collections.synchronizedMap(new LinkedHashMap<>());
        this.profilesToStore = Collections.synchronizedSet(new HashSet<>());
    }

    void clear() {
        synchronized (this.profiles) {
            synchronized (this.significancePie) {
                synchronized (this.profilesToStore) {
                    this.profiles.clear();
                    this.significancePie.clear();
                    this.storeProfiles(true);
                }
            }
        }
    }

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

    // Separator

    void cache(PlayerProfile profile,
               long time,
               long situation,
               double pattern,
               boolean store) {
        if (profile.isLegitimate()) {
            PatternProfile patternProfile = this.getProfile(profile);

            if (this.initiator.scissors != 0) {
                pattern = AlgebraUtils.cut(pattern, this.initiator.scissors);
            }
            if (patternProfile.patternRepetition.canUse(pattern)) {
                if (this.initiator.isEnabled()) {
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
                    this.customCache(patternProfile, situation, pattern);
                }

                if (store) {
                    this.parent.set(profile, time, situation, pattern);
                }
            }
        }
    }

    // Separator

    double averagePatternTime() {
        double time = 0.0;
        int profiles = 0;

        synchronized (this.profiles) {
            for (PatternProfile profile : this.profiles.values()) {
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

    public PatternProfile getProfile(PlayerProfile profile) {
        synchronized (this.profiles) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> (this.parent.linear ? new LinearPatternProfile(this, profile)
                            : new NonLinearPatternProfile(this, profile))
            );
        }
    }

    // Separator

    abstract protected boolean hasSituation(long situation);

    abstract protected void customCache(PatternProfile patternProfile, long situation, double pattern);

}
