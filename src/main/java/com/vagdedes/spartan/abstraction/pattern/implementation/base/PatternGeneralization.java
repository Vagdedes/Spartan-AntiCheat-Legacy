package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPie;

import java.util.*;

public abstract class PatternGeneralization { // todo optimize legitimate checks

    static final String
            profileOption = ".profile",
            patternOption = ".pattern",
            situationOption = ".situation";
    static final String[] options = {profileOption, patternOption, situationOption};
    static final Map<Long, PatternLinearCache> allCache = Collections.synchronizedMap(new LinkedHashMap<>());
    static final Map<Long, PatternNonLinearCache> rankedCache = Collections.synchronizedMap(new LinkedHashMap<>());

    // Separator

    final Pattern parent;
    final PatternGeneralizationInitiator initiator;
    final ProbabilityPie significancePie;
    final Map<PlayerProfile, PatternProfile> profiles;
    private final Collection<Collection<Double>> linearEmpty;
    private final Collection<Double> specificEmpty;
    private final ProbabilityPie nonLinearEmpty;
    final Set<PatternProfile> profilesToStore;

    protected PatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        this.parent = pattern;
        this.initiator = initiator;
        this.significancePie = new ProbabilityPie();
        this.profiles = Collections.synchronizedMap(new LinkedHashMap<>());
        this.linearEmpty = new ArrayList<>(0);
        this.specificEmpty = new ArrayList<>(0);
        this.nonLinearEmpty = new ProbabilityPie(0);
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
                synchronized (patternProfile.correlations) {
                    LinkedList<Double> list = patternProfile.correlations.computeIfAbsent(
                            situation,
                            k -> new LinkedList<>()
                    );
                    if (list.size() == this.initiator.square) {
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

    private void notify(SpartanPlayer player, long situation) {
        String message = AwarenessNotifications.getOptionalNotification(
                "Spartan's Machine Learning algorithms have insufficient data to check you. "
                        + (player.getProfile().isLegitimate()
                        ? "Continue playing LEGITIMATELY to train the algorithm so you can try again later."
                        : "Since you are " + player.getProfile().evidence.getType() + ", either clear your data via '/spartan info'"
                        + " and play LEGITIMATELY or find a legitimate player to help train the algorithm so you can try again later.")
        );

        if (message != null) {
            String s = "pattern-storage-" + this.parent.key;

            if (AwarenessNotifications.canSend(player.uuid, s + "-" + situation, 120)
                    && AwarenessNotifications.canSend(player.uuid, s, 60)) {
                player.sendMessage(message);
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
                return this.specificEmpty;
            } else {
                int size = specific.size();

                if (size < count) {
                    return this.specificEmpty;
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

    public Collection<Collection<Double>> getLinearData(SpartanPlayer player,
                                                        long situation,
                                                        short count) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + this.initiator.scissors;
        hash = (hash * SpartanBukkit.hashCodeMultiplierLong) + count;

        if (DetectionNotifications.isEnabled(player)) {
            Collection<Collection<Double>> result = this.getLinearDataRaw(situation, count);

            synchronized (allCache) {
                PatternLinearCache cache = allCache.get(hash);

                if (result.isEmpty()) {
                    notify(player, situation);
                }
                if (cache == null) {
                    cache = new PatternLinearCache(result);
                    allCache.put(hash, cache);
                    return cache.data;
                } else {
                    return cache.update(result);
                }
            }
        } else {
            synchronized (allCache) {
                PatternLinearCache cache = allCache.get(hash);

                if (cache == null) {
                    Collection<Collection<Double>> result = this.getLinearDataRaw(situation, count);
                    allCache.put(hash, new PatternLinearCache(result));
                    return result;
                } else if (cache.isExpired()) {
                    return cache.update(this.getLinearDataRaw(situation, count));
                } else {
                    return cache.data;
                }
            }
        }
    }

    private Collection<Collection<Double>> getLinearDataRaw(long situation, short count) {
        if (!SpartanBukkit.debug && this.profiles.size() < ResearchEngine.requiredProfiles) {
            return this.linearEmpty;
        } else {
            Collection<Collection<Double>> collections = new ArrayList<>(this.profiles.size());
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;

                        Collection<Double> data = entry.getValue().correlations.get(situation);

                        if (data != null && data.size() > count) {
                            collections.add(data);
                        }
                    }
                }
            }
            return !SpartanBukkit.debug && profiles < ResearchEngine.requiredProfiles
                    ? this.linearEmpty
                    : collections;
        }
    }

    // Separator

    public ProbabilityPie getNonLinearData(SpartanPlayer player,
                                           long situation) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + this.initiator.scissors;

        if (DetectionNotifications.isEnabled(player)) {
            ProbabilityPie result = this.getNonLinearDataRaw(situation);

            synchronized (rankedCache) {
                PatternNonLinearCache cache = rankedCache.get(hash);

                if (!result.hasData()) {
                    notify(player, situation);
                }
                if (cache == null) {
                    cache = new PatternNonLinearCache(result);
                    rankedCache.put(hash, cache);
                    return cache.data;
                } else {
                    return cache.update(result);
                }
            }
        } else {
            synchronized (rankedCache) {
                PatternNonLinearCache cache = rankedCache.get(hash);

                if (cache == null) {
                    cache = new PatternNonLinearCache(this.getNonLinearDataRaw(situation));
                    rankedCache.put(
                            hash,
                            cache
                    );
                    return cache.data;
                } else if (cache.isExpired()) {
                    return cache.update(this.getNonLinearDataRaw(situation));
                } else {
                    return cache.data;
                }
            }
        }
    }

    private ProbabilityPie getNonLinearDataRaw(long situation) {
        if (!SpartanBukkit.debug && this.profiles.size() < ResearchEngine.requiredProfiles) {
            return this.nonLinearEmpty;
        } else {
            ProbabilityPie rank = new ProbabilityPie();
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;
                        Collection<Double> data;

                        synchronized (entry.getValue().correlations) {
                            data = entry.getValue().correlations.get(situation);
                        }

                        if (data != null) {
                            rank.addMultiple(data);
                        }
                    }
                }
            }
            return !SpartanBukkit.debug && profiles < ResearchEngine.requiredProfiles
                    ? this.nonLinearEmpty
                    : rank;
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
                    k -> new PatternProfile(this, profile)
            );
        }
    }

}
