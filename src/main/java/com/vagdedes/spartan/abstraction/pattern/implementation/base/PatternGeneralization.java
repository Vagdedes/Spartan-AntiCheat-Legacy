package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.*;

public abstract class PatternGeneralization { // todo optimize legitimate checks

    static final String
            profileOption = ".profile",
            patternOption = ".pattern",
            situationOption = ".situation";
    static final String[] options = {profileOption, patternOption, situationOption};
    static final Map<Long, PatternAllCache> allCache = Collections.synchronizedMap(new LinkedHashMap<>());
    static final Map<Long, PatternRankCache> rankedCache = Collections.synchronizedMap(new LinkedHashMap<>());

    // Separator

    final Pattern parent;
    final PatternGeneralizationInitiator initiator;
    final ProbabilityPie significancePie;
    private final Collection<ProbabilityRank> comparisons;
    final Map<PlayerProfile, PatternProfile> profiles;
    private final Collection<Collection<Float>> allData;
    private final Collection<Float> specific;
    final Set<PatternProfile> profilesToStore;

    protected PatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        this.parent = pattern;
        this.initiator = initiator;
        this.significancePie = new ProbabilityPie();
        this.comparisons = new LinkedList<>(); // todo
        this.profiles = Collections.synchronizedMap(new LinkedHashMap<>());
        this.allData = new ArrayList<>(0);
        this.specific = new ArrayList<>(0);
        this.profilesToStore = Collections.synchronizedSet(new HashSet<>());
    }

    void clear() {
        synchronized (this.profiles) {
            synchronized (this.comparisons) { // todo
                synchronized (this.significancePie) {
                    synchronized (this.profilesToStore) {
                        this.profiles.clear();
                        this.comparisons.clear();
                        this.significancePie.clear();
                        this.profilesToStore.clear();
                    }
                }
            }
        }
    }

    void storeProfiles() {
        if (!this.profilesToStore.isEmpty()) {
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
                  float pattern,
                  boolean store) {
        if (this.initiator.scissors != 0) {
            pattern = AlgebraUtils.cut(pattern, this.initiator.scissors);
        }
        PatternProfile patternProfile = this.getProfile(profile);

        if (patternProfile.patternRepetition.canUse(pattern)) {
            patternProfile.lastContribution = time;

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
                    LinkedList<Float> list = patternProfile.correlations.computeIfAbsent(
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

    public Collection<Float> getSpecific(PatternProfile patternProfile,
                                         long situation,
                                         short count) {
        synchronized (patternProfile.correlations) {
            List<Float> specific = patternProfile.correlations.get(situation);

            if (specific == null) {
                return this.specific;
            } else {
                int size = specific.size();

                if (size < count) {
                    return this.specific;
                } else {
                    return new ArrayList<>(specific).subList(size - count, size);
                }
            }
        }
    }

    // Separator

    public Collection<Collection<Float>> getAll(SpartanPlayer player,
                                                long situation,
                                                short count) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + this.initiator.scissors;
        hash = (hash * SpartanBukkit.hashCodeMultiplierLong) + count;

        if (DetectionNotifications.isEnabled(player)) {
            Collection<Collection<Float>> result = this.getAllRaw(situation, count);

            synchronized (allCache) {
                PatternAllCache cache = allCache.get(hash);

                if (result.isEmpty()) {
                    notify(player, situation);
                }
                if (cache == null) {
                    cache = new PatternAllCache(result);
                    allCache.put(hash, cache);
                    return cache.data;
                } else {
                    return cache.update(result);
                }
            }
        } else {
            synchronized (allCache) {
                PatternAllCache cache = allCache.get(hash);

                if (cache == null) {
                    Collection<Collection<Float>> result = this.getAllRaw(situation, count);
                    allCache.put(hash, new PatternAllCache(result));
                    return result;
                } else if (cache.isExpired()) {
                    return cache.update(this.getAllRaw(situation, count));
                } else {
                    return cache.data;
                }
            }
        }
    }

    private Collection<Collection<Float>> getAllRaw(long situation, short count) {
        if (!SpartanBukkit.debug && this.profiles.size() < ResearchEngine.requiredProfiles) {
            return this.allData;
        } else {
            Collection<Collection<Float>> collections = new ArrayList<>(this.profiles.size());
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;

                        Collection<Float> data = entry.getValue().correlations.get(situation);

                        if (data != null && data.size() >= count) {
                            collections.add(data);
                        }
                    }
                }
            }
            return !SpartanBukkit.debug && profiles < ResearchEngine.requiredProfiles
                    ? this.allData
                    : collections;
        }
    }

    // Separator

    public ProbabilityRank getRanked(SpartanPlayer player,
                                     long situation) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + this.initiator.scissors;

        if (DetectionNotifications.isEnabled(player)) {
            ProbabilityRank result = this.getRankedRaw(situation);

            synchronized (rankedCache) {
                PatternRankCache cache = rankedCache.get(hash);

                if (!result.hasData()) {
                    notify(player, situation);
                }
                if (cache == null) {
                    cache = new PatternRankCache(result);
                    rankedCache.put(hash, cache);
                    return cache.data;
                } else {
                    return cache.update(result);
                }
            }
        } else {
            synchronized (rankedCache) {
                PatternRankCache cache = rankedCache.get(hash);

                if (cache == null) {
                    cache = new PatternRankCache(this.getRankedRaw(situation));
                    rankedCache.put(
                            hash,
                            cache
                    );
                    return cache.data;
                } else if (cache.isExpired()) {
                    return cache.update(this.getRankedRaw(situation));
                } else {
                    return cache.data;
                }
            }
        }
    }

    private ProbabilityRank getRankedRaw(long situation) {
        if (!SpartanBukkit.debug && this.profiles.size() < ResearchEngine.requiredProfiles) {
            return new ProbabilityRank();
        } else {
            ProbabilityRank rank = new ProbabilityRank();
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;
                        Collection<Float> data;

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
                    ? new ProbabilityRank()
                    : rank;
        }
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
