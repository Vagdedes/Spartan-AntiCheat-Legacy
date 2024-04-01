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
    private final ProbabilityPie significancePie;
    private final PatternComparison
            trustFactorComparison,
            contributionComparison;
    final PatternComparison[] patternComparisons;
    final Map<PlayerProfile, PatternProfile> profiles;
    private final Collection<Collection<Number>> allData;
    private final Collection<Number> specific;

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
        this.allData = new ArrayList<>(0);
        this.specific = new ArrayList<>(0);
    }

    void clear() {
        synchronized (this.profiles) {
            synchronized (this.trustFactorComparison) {
                synchronized (this.contributionComparison) {
                    synchronized (this.significancePie) {
                        this.profiles.clear();
                        this.trustFactorComparison.clear();
                        this.contributionComparison.clear();
                        this.significancePie.clear();
                    }
                }
            }
        }
    }

    public double[] trustFactorRankAndProbability(PatternProfile patternProfile) {
        double value;

        synchronized (patternProfile.comparisons) {
            value = patternProfile.trustFactor(true);
        }
        return this.trustFactorComparison.rankAndProbability(value);
    }

    public double[] contributionRankAndProbability(PatternProfile patternProfile) {
        double value;

        synchronized (patternProfile.comparisons) {
            value = patternProfile.contribution(true);
        }
        return this.contributionComparison.rankAndProbability(value);
    }

    // Separator

    void rawCache(PlayerProfile profile,
                  long time,
                  long situation,
                  Number pattern,
                  boolean store) {
        if (this.initiator.generalization != 0) {
            pattern = AlgebraUtils.generalize(pattern, this.initiator.generalization);
        }
        PatternProfile patternProfile = this.getProfile(profile);

        if (patternProfile.patternRepetition.canUse(pattern)) {
            if (this.significancePie.getSlices() == this.initiator.generalWeight) {
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
                LinkedList<Number> list = patternProfile.correlations.computeIfAbsent(
                        situation,
                        k -> new LinkedList<>()
                );
                if (list.size() == this.initiator.individualGeneralWeight) {
                    list.removeFirst();
                }
                list.add(pattern);
            }

            if (store) {
                ((PatternGeneralizationStorage) this).set(patternProfile, time, situation, pattern);
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

    public Collection<Number> getSpecific(PatternProfile patternProfile,
                                          long situation,
                                          int count) {
        synchronized (patternProfile.correlations) {
            List<Number> specific = patternProfile.correlations.get(situation);

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

    public Collection<Collection<Number>> getAll(SpartanPlayer player,
                                                 long situation,
                                                 int count) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + this.initiator.generalization;
        hash = (hash * SpartanBukkit.hashCodeMultiplierLong) + count;

        if (DetectionNotifications.isEnabled(player)) {
            Collection<Collection<Number>> result = this.getAllRaw(situation, count);

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
                    Collection<Collection<Number>> result = this.getAllRaw(situation, count);
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

    private Collection<Collection<Number>> getAllRaw(long situation, int count) {
        if (!SpartanBukkit.debug && this.profiles.size() < ResearchEngine.requiredProfiles) {
            return this.allData;
        } else {
            Collection<Collection<Number>> collections = new ArrayList<>(this.profiles.size());
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;

                        Collection<Number> data = entry.getValue().correlations.get(situation);

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
            return new ProbabilityRank();
        } else {
            ProbabilityRank rank = new ProbabilityRank();
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;
                        Collection<Number> data;

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

    public void compare(PatternProfile patternProfile,
                        Number number,
                        double probability,
                        long contribution,
                        long time) {
        synchronized (patternProfile.comparisons) {
            synchronized (this.significancePie) {
                patternProfile.refreshComparisons(
                        time,
                        probability * this.significancePie.getProbability(number),
                        contribution
                );
            }
            this.trustFactorComparison.put(
                    patternProfile,
                    patternProfile.trustFactor(false),
                    time,
                    true
            );
            this.contributionComparison.put(
                    patternProfile,
                    patternProfile.contribution(false),
                    time,
                    true
            );
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
