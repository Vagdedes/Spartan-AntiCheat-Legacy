package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.*;

public abstract class PatternGeneralization { // todo fix hacker data removal

    static final String
            profileOption = ".profile",
            patternOption = ".pattern",
            situationOption = ".situation";
    static final String[] options = {profileOption, patternOption, situationOption};
    static final Map<Long, PatternNonLinearCache> nonLinearCache = Collections.synchronizedMap(new LinkedHashMap<>());

    // Separator

    final Pattern parent;
    final PatternGeneralizationInitiator initiator;
    final ProbabilityPie significancePie;
    final Map<PlayerProfile, PatternProfile> profiles;
    private final Collection<Double> empty;
    private final ProbabilityPie nonLinearEmpty;
    final Set<PatternProfile> profilesToStore;
    final Map<Long, LinkedList<Double>> correlations;

    protected PatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        this.parent = pattern;
        this.initiator = initiator;
        this.significancePie = new ProbabilityPie();
        this.profiles = Collections.synchronizedMap(new LinkedHashMap<>());
        this.empty = new ArrayList<>(0);
        this.nonLinearEmpty = new ProbabilityPie(0);
        this.profilesToStore = Collections.synchronizedSet(new HashSet<>());
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
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

    void rawCache(PlayerProfile profile,
                  long time,
                  long situation,
                  double pattern,
                  boolean store) {
        if (this.initiator.scissors != 0) {
            pattern = AlgebraUtils.cut(pattern, this.initiator.scissors);
        }
        PatternProfile patternProfile = this.getProfile(profile);

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
                synchronized (this.correlations) {
                    LinkedList<Double> list = this.correlations.computeIfAbsent(
                            situation,
                            k -> new LinkedList<>()
                    );
                    if (list.size() == this.initiator.cube) {
                        list.removeFirst();
                    }
                }
                synchronized (patternProfile.correlations) {
                    LinkedList<Double> list = patternProfile.correlations.computeIfAbsent(
                            situation,
                            k -> new LinkedList<>()
                    );
                    if (list.size() == this.initiator.line) {
                        list.removeFirst();
                    }
                    list.add(pattern);
                }
            }

            if (store) {
                ((PatternGeneralizationStorage) this).set(profile, time, situation, pattern);
            }
        }
    }

    // Separator

    public Collection<Double> getSpecificLinearData(PatternProfile patternProfile,
                                                    long situation,
                                                    short count) {
        synchronized (patternProfile.correlations) {
            LinkedList<Double> specific = patternProfile.correlations.get(situation);

            if (specific == null) {
                return this.empty;
            } else {
                int size = specific.size();

                if (size < count) {
                    return this.empty;
                } else if (count == 1) {
                    Collection<Double> collection = new ArrayList<>(1);
                    collection.add(specific.getLast());
                    return collection;
                } else {
                    return new ArrayList<>(specific).subList(size - count, size);
                }
            }
        }
    }

    // Separator

    public Collection<Double> getLinearData(long situation) {
        synchronized (this.correlations) {
            return this.correlations.getOrDefault(situation, new LinkedList<>());
        }
    }

    public ProbabilityPie getNonLinearData(long situation) { // todo fix to be stored this way based on object
        return new ProbabilityPie().add(
                this.correlations.getOrDefault(situation, new LinkedList<>())
        );
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
                    k -> new PatternProfile(this, profile)
            );
        }
    }

}
