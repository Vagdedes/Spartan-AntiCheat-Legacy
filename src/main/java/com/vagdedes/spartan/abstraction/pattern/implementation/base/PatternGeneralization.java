package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.*;

public abstract class PatternGeneralization {

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
    final PatternComparison
            trustFactorComparison,
            contributionComparison;
    final PatternComparison[] patternComparisons;
    final Map<PlayerProfile, PatternProfile> profiles;
    private final PatternCache patternSizeCache;

    protected PatternGeneralization(Pattern pattern, PatternGeneralizationInitiator initiator) {
        this.parent = pattern;
        this.initiator = initiator;
        this.significancePie = new ProbabilityPie();
        this.trustFactorComparison = new PatternComparison(this, "trust");
        this.contributionComparison = new PatternComparison(this, "contribution");
        this.patternComparisons = new PatternComparison[]{
                this.trustFactorComparison,
                this.contributionComparison
        };
        this.profiles = Collections.synchronizedMap(new LinkedHashMap<>());
        this.patternSizeCache = new PatternCache();
    }

    public double[] trustFactorRankAndProbability(PatternProfile patternProfile) {
        double value;

        synchronized (patternProfile.trustFactorMemory) {
            value = patternProfile.trustFactor();
        }
        synchronized (this.trustFactorComparison) {
            return this.trustFactorComparison.rankAndProbability(value);
        }
    }

    public double[] contributionRankAndProbability(PatternProfile patternProfile) {
        double value;

        synchronized (patternProfile.contribution) {
            value = patternProfile.contribution();
        }
        synchronized (this.contributionComparison) {
            return this.contributionComparison.rankAndProbability(value);
        }
    }

    // Separator

    void rawCache(PlayerProfile profile,
                  PatternValue value,
                  boolean significant,
                  boolean store) {
        PatternProfile patternProfile = this.getProfile(profile);

        if (patternProfile.patternRepetition.canUse(value.pattern)) {
            List<PatternValue> list = patternProfile.correlations.computeIfAbsent(
                    value.situation,
                    k -> new ArrayList<>(100)
            );
            list.add(value);

            if (significant) {
                this.significancePie.add(value.pattern);
            }

            if (patternSizeCache.isExpired()) {
                patternSizeCache.refresh();
                int dataLimit = Math.min(
                        SpartanEdition.is2ndGeneration() ? 1_000_000 : 100_000,
                        Cache.maxBytes / this.parent.generalizations.size() / patternProfile.contribution.size() / PatternValue.objectSize
                ),
                        delete = list.size() - dataLimit;

                if (delete > 0) {
                    Iterator<PatternValue> iterator = list.iterator();

                    while (iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();

                        if (--delete == 0) {
                            break;
                        }
                    }
                }
                delete = this.significancePie.getSlices() - dataLimit;

                if (delete > 0) {
                    this.significancePie.removeLeastSignificant(delete);
                }
            }

            if (store) {
                ((PatternGeneralizationStorage) this).set(patternProfile, value);
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

    public List<PatternValue> getSpecific(PatternProfile patternProfile,
                                          long situation,
                                          int count) {
        synchronized (patternProfile.correlations) {
            List<PatternValue> specific = patternProfile.correlations.get(situation);

            if (specific == null) {
                return new ArrayList<>(0);
            } else {
                int size = specific.size();

                if (size < count) {
                    return new ArrayList<>(0);
                } else {
                    return new ArrayList<>(specific).subList(size - count, size);
                }
            }
        }
    }

    // Separator

    public Collection<Collection<PatternValue>> getAll(SpartanPlayer player,
                                                       long situation) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + this.initiator.generalization;

        if (DetectionNotifications.isEnabled(player)) {
            Collection<Collection<PatternValue>> result = this.getAllRaw(situation);

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
                    Collection<Collection<PatternValue>> result = this.getAllRaw(situation);
                    allCache.put(hash, new PatternAllCache(result));
                    return result;
                } else if (cache.isExpired()) {
                    return cache.update(this.getAllRaw(situation));
                } else {
                    return cache.data;
                }
            }
        }
    }

    private Collection<Collection<PatternValue>> getAllRaw(long situation) {
        if (!SpartanBukkit.debug && this.profiles.size() < ResearchEngine.requiredProfiles) {
            return new ArrayList<>(0);
        } else {
            Collection<Collection<PatternValue>> collections = new ArrayList<>(this.profiles.size());
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;

                        Collection<PatternValue> data = entry.getValue().correlations.get(situation);

                        if (data != null) {
                            collections.add(data);
                        }
                    }
                }
            }
            return !SpartanBukkit.debug && profiles < ResearchEngine.requiredProfiles
                    ? new ArrayList<>(0)
                    : collections;
        }
    }

    // Separator

    public ProbabilityRank getRanked(SpartanPlayer player,
                                     long situation) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + this.initiator.generalization;

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
            return new ProbabilityRank(0);
        } else {
            ProbabilityRank rank = new ProbabilityRank(this.profiles.size());
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;

                        Collection<PatternValue> data = entry.getValue().correlations.get(situation);

                        if (data != null) {
                            rank.addMultiplePatterns(data);
                        }
                    }
                }
            }
            return !SpartanBukkit.debug && profiles < ResearchEngine.requiredProfiles
                    ? new ProbabilityRank(0)
                    : rank;
        }
    }

    // Separator

    public void compare(PatternProfile patternProfile,
                        long contribution,
                        long time) {
        synchronized (patternProfile.contribution) {
            patternProfile.contribution.add(contribution);

            synchronized (this.contributionComparison) {
                this.contributionComparison.put(
                        patternProfile,
                        patternProfile.contribution(),
                        time,
                        true
                );
            }
        }
    }

    public void compare(PatternProfile patternProfile,
                        Number number,
                        double probability,
                        long time) {
        synchronized (patternProfile.trustFactorMemory) {
            patternProfile.addTrustFactor(probability * this.significancePie.getProbability(number));

            synchronized (this.trustFactorComparison) {
                this.trustFactorComparison.put(
                        patternProfile,
                        patternProfile.trustFactor(),
                        time,
                        true
                );
            }
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
