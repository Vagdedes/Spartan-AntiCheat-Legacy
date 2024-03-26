package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;

import java.util.*;

public abstract class PatternGeneralization {

    static boolean include(SpartanPlayer player) {
        Integer divisor = DetectionNotifications.getDivisor(player, false);
        return divisor == null || divisor != 69;
    }

    static Number generalize(Number number, int generalization) {
        if (generalization == 0) {
            return number;
        } else if (number instanceof Double) {
            return AlgebraUtils.cut(number.doubleValue(), generalization);
        } else if (number instanceof Float) {
            return AlgebraUtils.cut(number.floatValue(), generalization);
        } else if (number instanceof Long) {
            return AlgebraUtils.roundToNearest(number.longValue(), generalization);
        } else if (number instanceof Integer) {
            return AlgebraUtils.roundToNearest(number.intValue(), generalization);
        } else {
            return AlgebraUtils.roundToNearest(number.shortValue(), generalization);
        }
    }

    static final String
            profileOption = ".profile",
            patternOption = ".pattern",
            situationOption = ".situation";
    static final String[] options = {profileOption, patternOption, situationOption};

    static final Map<Long, PatternAllCache> allCache = Collections.synchronizedMap(new LinkedHashMap<>());
    static final Map<Long, PatternRankCache> rankedCache = Collections.synchronizedMap(new LinkedHashMap<>());

    // Separator

    final Pattern parent;
    final int generalization;
    final ProbabilityPie significancePie;
    final PatternComparison
            probabilityRank,
            significanceRank,
            contributionRank;
    final PatternComparison[] patternComparisons;
    final Map<PlayerProfile, PatternProfile> profiles;
    private final PatternCache patternSizeCache;

    protected PatternGeneralization(Pattern pattern, int generalization) {
        this.parent = pattern;
        this.generalization = generalization;
        this.significancePie = new ProbabilityPie();
        this.probabilityRank = new PatternComparison(this, "probability");
        this.significanceRank = new PatternComparison(this, "significance");
        this.contributionRank = new PatternComparison(this, "contribution");
        this.patternComparisons = new PatternComparison[]{
                this.probabilityRank,
                this.significanceRank,
                this.contributionRank
        };
        this.profiles = Collections.synchronizedMap(new LinkedHashMap<>());
        this.patternSizeCache = new PatternCache();
    }

    public double[] probabilityRankAndProbability(PlayerProfile profile) {
        synchronized (this.probabilityRank) {
            return this.rankAndProbability(this.probabilityRank, this.getProfile(profile).probability());
        }
    }

    public double[] significanceRankAndProbability(PlayerProfile profile) {
        synchronized (this.significanceRank) {
            return this.rankAndProbability(this.significanceRank, this.getProfile(profile).significance());
        }
    }

    public double[] contributionRankAndProbability(PlayerProfile profile) {
        synchronized (this.contributionRank) {
            return this.rankAndProbability(this.contributionRank, this.getProfile(profile).contribution());
        }
    }

    private double[] rankAndProbability(PatternComparison object, double value) {
        if (value == -1.0) {
            return null;
        } else {
            double rank = object.getPosition(value);

            if (rank != -1.0) {
                return new double[]{rank, object.getProbability(value)};
            } else {
                return null;
            }
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
                int dataLimit = SpartanEdition.is2ndGeneration() ? 1_000_000 : 100_000,
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
                delete = this.significancePie.getTotal() - dataLimit;

                if (delete > 0) {
                    this.significancePie.removeOld(delete);
                }
            }

            if (store) {
                ((PatternGeneralizationStorage) this).set(patternProfile, value);
            }
        }
    }

    // Separator

    public double getSignificance(Number number) {
        return this.significancePie.getProbability(number);
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

    public List<PatternValue> getSpecific(PlayerProfile profile,
                                          long situation,
                                          int count) {
        synchronized (this.profiles) {
            PatternProfile patternProfile = this.profiles.get(profile);

            if (patternProfile == null) {
                return new ArrayList<>(0);
            } else {
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
    }

    // Separator

    public Collection<Collection<PatternValue>> getAll(SpartanPlayer player,
                                                       long situation) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + this.generalization;

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
        if (!SpartanBukkit.debug && this.profiles.size() < TPS.maximum) {
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
            return !SpartanBukkit.debug && profiles < TPS.maximum
                    ? new ArrayList<>(0)
                    : collections;
        }
    }

    // Separator

    public ProbabilityRank getRanked(SpartanPlayer player,
                                     long situation) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + this.generalization;

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

    public ProbabilityRank getRankedRaw(long situation) {
        if (!SpartanBukkit.debug && this.profiles.size() < TPS.maximum) {
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
            return !SpartanBukkit.debug && profiles < TPS.maximum
                    ? new ProbabilityRank(0)
                    : rank;
        }
    }

    // Separator

    public void compare(SpartanPlayer player,
                        long contribution,
                        long time) {
        boolean include = include(player);

        synchronized (this.profiles) {
            PatternProfile patternProfile = getProfile(player.getProfile());

            synchronized (patternProfile.contribution) {
                patternProfile.contribution.add(contribution);

                if (include) {
                    synchronized (this.contributionRank) {
                        this.contributionRank.put(
                                patternProfile,
                                patternProfile.contributionRaw(),
                                time,
                                true
                        );
                    }
                }
            }
        }
    }

    public void compare(SpartanPlayer player,
                        double probability, double significance,
                        long time) {
        boolean include = include(player);

        synchronized (this.profiles) {
            PatternProfile patternProfile = getProfile(player.getProfile());

            synchronized (patternProfile.mutual) {
                patternProfile.mutual.add(probability, significance);

                if (include) {
                    synchronized (this.probabilityRank) {
                        this.probabilityRank.put(
                                patternProfile,
                                patternProfile.probabilityRaw(),
                                time,
                                true
                        );
                    }
                    synchronized (this.significanceRank) {
                        this.significanceRank.put(
                                patternProfile,
                                patternProfile.significanceRaw(),
                                time,
                                true
                        );
                    }
                }
            }
        }
    }

    // Separator

    private PatternProfile getProfile(PlayerProfile profile) {
        synchronized (this.profiles) {
            return this.profiles.computeIfAbsent(
                    profile,
                    k -> new PatternProfile(this, profile)
            );
        }
    }
}
