package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternStorage {

    private static final Collection<PatternStorage> instances = Collections.synchronizedList(new ArrayList<>());
    private static final Map<Long, PatternAllCache> allCache = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Long, PatternRankCache> rankedCache = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void delete(PlayerProfile profile) {
        synchronized (instances) {
            for (PatternStorage instance : instances) {
                synchronized (instance.profiles) {
                    if (instance.profiles.remove(profile) != null) {
                        File directory = new File(PatternGeneralization.path(instance.key));

                        if (directory.isDirectory()) {
                            File[] files = directory.listFiles();

                            if (files != null) {
                                for (File file : files) {
                                    if (file.isFile()
                                            && file.getName().endsWith(".yml")) {
                                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                                        Set<String> keys = configuration.getKeys(false);

                                        if (!keys.isEmpty()) {
                                            boolean found = false;

                                            for (String key : keys) {
                                                try {
                                                    String profileName = configuration.getString(key + PatternGeneralization.profileOption, null);

                                                    if (profileName != null
                                                            && profileName.equals(profile.getName())) {
                                                        for (String option : PatternGeneralization.options) {
                                                            configuration.set(key + option, null);
                                                        }
                                                        configuration.set(key, null);
                                                        found = true;
                                                    }
                                                } catch (Exception ignored) {
                                                }
                                            }

                                            if (found) {
                                                try {
                                                    configuration.save(file);
                                                } catch (Exception ignored) {
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void reload() {
        synchronized (instances) {
            for (PatternStorage patternStorage : instances) {
                patternStorage.load();
            }
        }
    }

    public static void clear() {
        synchronized (instances) {
            synchronized (allCache) {
                synchronized (rankedCache) {
                    instances.clear();
                    allCache.clear();
                    rankedCache.clear();
                }
            }
        }
    }

    // Separator

    public final String key;
    private final Map<Integer, PatternGeneralization> generalizations;
    final Map<PlayerProfile, PatternProfile> profiles;
    private final PatternPieCache patternProbability;

    public PatternStorage(String key, int[] generalizations) {
        this.key = key;
        this.patternProbability = new PatternPieCache(new ProbabilityPie());
        this.profiles = Collections.synchronizedMap(new LinkedHashMap<>());

        this.generalizations = new LinkedHashMap<>(generalizations.length + 1);
        this.generalizations.put(0, new PatternGeneralizationStorage(key));

        if (generalizations.length > 0) {
            for (int generalization : generalizations) {
                this.generalizations.put(generalization, new PatternGeneralizationMemory(key, generalization));
            }
        }

        synchronized (instances) {
            instances.add(this);
        }
    }

    // Separator

    public void store() {
        ((PatternGeneralizationStorage) this.generalizations.get(0)).store();
    }

    private void load() {
        store();
        Map<PlayerProfile, PatternProfile> correlations = new LinkedHashMap<>();
        File directory = new File(PatternGeneralization.path(this.key));

        if (!directory.exists()) {
            directory.mkdirs();
        } else if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));

                for (File file : files) {
                    if (file.isFile()
                            && file.getName().endsWith(".yml")) {
                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                        Set<String> keys = configuration.getKeys(false);

                        if (!keys.isEmpty()) {
                            for (String key : keys) {
                                try {
                                    long time = Long.parseLong(key);
                                    Object pattern = configuration.get(key + PatternGeneralization.patternOption, null);

                                    if (pattern instanceof Number) {
                                        long situation = configuration.getLong(key + PatternGeneralization.situationOption, Long.MIN_VALUE);

                                        if (situation != Long.MIN_VALUE) {
                                            String profileName = configuration.getString(key + PatternGeneralization.profileOption, null);

                                            if (profileName != null
                                                    && !profileName.isEmpty()) {
                                                PlayerProfile profile = ResearchEngine.getPlayerProfile(profileName);

                                                for (PatternGeneralization generalization : this.generalizations.values()) {
                                                    this.rawCache(
                                                            correlations,
                                                            profile,
                                                            new PatternValue(
                                                                    situation,
                                                                    (Number) pattern,
                                                                    generalization.generalization,
                                                                    time
                                                            ),
                                                            generalization,
                                                            false
                                                    );
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            } else {
                directory.delete();
                directory.mkdirs();
            }
        }

        synchronized (this.profiles) {
            this.profiles.clear();
            this.profiles.putAll(correlations);
        }
    }

    // Separator

    private void rawCache(Map<PlayerProfile, PatternProfile> correlations,
                          PlayerProfile profile,
                          PatternValue value,
                          PatternGeneralization generalization,
                          boolean set) {
        PatternProfile patternProfile = correlations.computeIfAbsent(
                profile,
                k -> new PatternProfile()
        );

        synchronized (patternProfile.correlations) {
            List<PatternValue> list = patternProfile.correlationsData(
                    generalization.generalization
            ).computeIfAbsent(
                    value.situation,
                    k -> new ArrayList<>(100)
            );
            list.add(value);

            if (list.size() > (SpartanEdition.is2ndGeneration() ? 3_000_000 : 1_000_000)) {
                list.remove(0);
            }
        }

        if (set) {
            ((PatternGeneralizationStorage) generalization).set(profile, value);
        }
    }

    public void collect(PlayerProfile profile, long situation, Number number, boolean collect) {
        if (collect) {
            long time = System.currentTimeMillis();

            synchronized (this.profiles) {
                PatternProfile statistics = this.profiles.get(profile);

                if (statistics == null) {
                    statistics = new PatternProfile();
                    statistics.repetitions.put(situation, new PatternRepetition(number));
                    this.profiles.put(profile, statistics);
                } else if (!statistics.canUsePattern(situation, number)) {
                    return;
                }
                for (PatternGeneralization generalization : this.generalizations.values()) {
                    this.rawCache(
                            this.profiles,
                            profile,
                            new PatternValue(
                                    situation,
                                    number,
                                    generalization.generalization,
                                    time
                            ),
                            generalization,
                            generalization instanceof PatternGeneralizationStorage
                    );
                }
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
            String s = "pattern-storage-" + key;

            if (AwarenessNotifications.canSend(player.uuid, s + "-" + situation, 120)
                    && AwarenessNotifications.canSend(player.uuid, s, 60)) {
                player.sendMessage(message);
            }
        }
    }

    // Separator

    public Collection<Collection<PatternValue>> getAll(SpartanPlayer player, long situation, int generalization) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong) + generalization;

        if (DetectionNotifications.isEnabled(player)) {
            Collection<Collection<PatternValue>> result = this.getAllRaw(situation, generalization);

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
                    Collection<Collection<PatternValue>> result = this.getAllRaw(situation, generalization);
                    allCache.put(hash, new PatternAllCache(result));
                    return result;
                } else if (cache.isExpired()) {
                    return cache.update(this.getAllRaw(situation, generalization));
                } else {
                    return cache.data;
                }
            }
        }
    }

    private Collection<Collection<PatternValue>> getAllRaw(long situation, int generalization) {
        if (!SpartanBukkit.debug && this.profiles.size() < ResearchEngine.consistentDataRequirement) {
            return new ArrayList<>(0);
        } else {
            Collection<Collection<PatternValue>> collections = new ArrayList<>(this.profiles.size());
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;

                        synchronized (entry.getValue().correlations) {
                            Collection<PatternValue> data = entry.getValue().correlationsData(generalization).get(situation);

                            if (data != null) {
                                collections.add(data);
                            }
                        }
                    }
                }
            }
            return !SpartanBukkit.debug && profiles < ResearchEngine.consistentDataRequirement
                    ? new ArrayList<>(0)
                    : collections;
        }
    }

    // Separator

    public List<PatternValue> getSpecific(PlayerProfile profile, long situation, int generalization, int count) {
        synchronized (this.profiles) {
            PatternProfile patternProfile = this.profiles.get(profile);

            if (patternProfile == null) {
                return new ArrayList<>(0);
            } else {
                synchronized (patternProfile.correlations) {
                    List<PatternValue> specific = patternProfile.correlationsData(generalization).get(situation);

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
    }

    // Separator

    public ProbabilityRank getRanked(SpartanPlayer player, long situation, int generalization) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong) + generalization;

        if (DetectionNotifications.isEnabled(player)) {
            ProbabilityRank result = this.getRankedRaw(situation, generalization);

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
                    cache = new PatternRankCache(this.getRankedRaw(situation, generalization));
                    rankedCache.put(
                            hash,
                            cache
                    );
                    return cache.data;
                } else if (cache.isExpired()) {
                    return cache.update(this.getRankedRaw(situation, generalization));
                } else {
                    return cache.data;
                }
            }
        }
    }

    public ProbabilityRank getRankedRaw(long situation, int generalization) {
        if (!SpartanBukkit.debug && this.profiles.size() < ResearchEngine.consistentDataRequirement) {
            return new ProbabilityRank(0);
        } else {
            ProbabilityRank rank = new ProbabilityRank(this.profiles.size());
            int profiles = 0;

            synchronized (this.profiles) {
                for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                    if (entry.getKey().isLegitimate()) {
                        profiles++;

                        synchronized (entry.getValue().correlations) {
                            Collection<PatternValue> data = entry.getValue().correlationsData(generalization).get(situation);

                            if (data != null) {
                                rank.addMultiplePatterns(data);
                            }
                        }
                    }
                }
            }
            return !SpartanBukkit.debug && profiles < ResearchEngine.consistentDataRequirement
                    ? new ProbabilityRank(0)
                    : rank;
        }
    }

    // Separator

    public double getSignificance(Number number, int generalization) {
        synchronized (this.patternProbability) {
            if (this.patternProbability.isExpired()) {
                this.patternProbability.data.clear();

                synchronized (this.profiles) {
                    for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                        if (entry.getKey().isLegitimate()) {
                            synchronized (entry.getValue().correlations) {
                                Map<Long, List<PatternValue>> data = entry.getValue().correlationsData(generalization);

                                if (!data.isEmpty()) {
                                    for (List<PatternValue> list : data.values()) {
                                        for (PatternValue value : list) {
                                            this.patternProbability.data.add(value.pattern);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            this.patternProbability.refresh();
        }
        return patternProbability.data.getProbability(number);
    }

    public PatternGeneralization getGeneralization(int generalization) {
        return this.generalizations.get(generalization);
    }

    // Separator

    public void collect(PlayerProfile profile, int generalization, long time) {
        PatternGeneralization generalizationStorage = this.generalizations.get(generalization);

        synchronized (this.profiles) {
            PatternProfile patternProfile = this.profiles.computeIfAbsent(
                    profile,
                    k -> new PatternProfile()
            );
            synchronized (patternProfile.contribution) {
                patternProfile.contributionData(generalization).add(time);

                if (time != -1L) {
                    synchronized (generalizationStorage.contributionRank) {
                        double value = patternProfile.contributionRaw(generalization);

                        if (value != -1.0) {
                            generalizationStorage.contributionRank.put(profile, value, time);
                        } else {
                            generalizationStorage.contributionRank.remove(profile);
                        }
                    }
                }
            }
        }
    }

    public void collect(PlayerProfile profile,
                        int generalization,
                        double significance, double probability,
                        long time) {
        PatternGeneralization generalizationStorage = this.generalizations.get(generalization);

        synchronized (this.profiles) {
            PatternProfile patternProfile = this.profiles.computeIfAbsent(
                    profile,
                    k -> new PatternProfile()
            );

            synchronized (patternProfile.probability) {
                patternProfile.total[PatternProfile.PROBABILITY_POSITION] += probability;
                patternProfile.probabilityData(generalization).add(probability);

                synchronized (generalizationStorage.probabilityRank) {
                    double value = patternProfile.probabilityRaw(generalization);

                    if (value != -1.0) {
                        generalizationStorage.probabilityRank.put(profile, value, time);
                    } else {
                        generalizationStorage.probabilityRank.remove(profile);
                    }
                }
            }
            synchronized (patternProfile.significance) {
                patternProfile.total[PatternProfile.SIGNIFICANCE_POSITION] += significance;
                patternProfile.significanceData(generalization).add(significance);

                synchronized (generalizationStorage.significanceRank) {
                    double value = patternProfile.significanceRaw(generalization);

                    if (value != -1.0) {
                        generalizationStorage.significanceRank.put(profile, value, time);
                    } else {
                        generalizationStorage.significanceRank.remove(profile);
                    }
                }
            }
        }
    }

    public PatternProfile getProfile(PlayerProfile profile) {
        synchronized (this.profiles) {
            return this.profiles.computeIfAbsent(profile, k -> new PatternProfile());
        }
    }
}
