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

public class Pattern {

    private static final Collection<Pattern> instances = Collections.synchronizedList(new ArrayList<>());
    private static final Map<Long, PatternAllCache> allCache = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Long, PatternRankCache> rankedCache = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void delete(PlayerProfile profile) {
        synchronized (instances) {
            for (Pattern instance : instances) {
                synchronized (instance.profiles) {
                    PatternProfile patternProfile = instance.profiles.remove(profile);

                    if (patternProfile != null) {
                        patternProfile.storage.delete();
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
            for (Pattern patternStorage : instances) {
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
    final Set<PatternProfileStorage.GeneralizationFile> profileFiles;
    private final PatternPieCache patternProbability;
    private final PatternCache patternSizeCache;

    public Pattern(String key, int[] generalizations) {
        this.key = key;
        this.patternProbability = new PatternPieCache(new ProbabilityPie());
        this.patternSizeCache = new PatternCache();
        this.profiles = Collections.synchronizedMap(new LinkedHashMap<>());
        this.profileFiles = Collections.synchronizedSet(new HashSet<>());

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

        if (!this.profileFiles.isEmpty()) {
            synchronized (this.profileFiles) {
                Iterator<PatternProfileStorage.GeneralizationFile> iterator = this.profileFiles.iterator();

                while (iterator.hasNext()) {
                    iterator.next().store();
                    iterator.remove();
                }
            }
        }
    }

    private void load() {
        store();
        Map<PlayerProfile, PatternProfile> correlations = new LinkedHashMap<>();
        File directory = new File(PatternGeneralization.path(this.key));

        if (!directory.exists()) {
            SpartanBukkit.dataThread.execute(() -> directory.mkdirs());
        } else if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));

                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".yml")) {
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
                                                                    generalization,
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
                SpartanBukkit.dataThread.execute(() -> {
                    directory.delete();
                    directory.mkdirs();
                });
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
                          boolean store) {
        PatternProfile patternProfile = correlations.computeIfAbsent(
                profile,
                k -> new PatternProfile(this, profile)
        );

        synchronized (patternProfile.correlations) {
            List<PatternValue> list = patternProfile.correlationsData(
                    generalization
            ).computeIfAbsent(
                    value.situation,
                    k -> new ArrayList<>(100)
            );
            list.add(value);

            if (patternSizeCache.isExpired()) {
                patternSizeCache.refresh();
                int delete = list.size() - (SpartanEdition.is2ndGeneration() ? 1_000_000 : 100_000);

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
            }
        }

        if (store) {
            ((PatternGeneralizationStorage) generalization).set(patternProfile, value);
        }
    }

    public void learn(SpartanPlayer player, long situation, Number number, boolean collect) {
        if (collect) {
            long time = System.currentTimeMillis();
            boolean include = include(player);

            synchronized (this.profiles) {
                PatternProfile statistics = this.profiles.get(player.getProfile());

                if (statistics == null) {
                    statistics = new PatternProfile(this, player.getProfile());
                    statistics.repetitions.put(situation, new PatternRepetition(number));
                    this.profiles.put(player.getProfile(), statistics);
                } else if (!statistics.canUsePattern(situation, number)) {
                    return;
                }
                for (PatternGeneralization generalization : this.generalizations.values()) {
                    this.rawCache(
                            this.profiles,
                            player.getProfile(),
                            new PatternValue(
                                    situation,
                                    number,
                                    generalization,
                                    time
                            ),
                            generalization,
                            include && generalization instanceof PatternGeneralizationStorage
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

    private boolean include(SpartanPlayer player) {
        Integer divisor = DetectionNotifications.getDivisor(player, false);
        return divisor == null || divisor != 69;
    }

    // Separator

    public Collection<Collection<PatternValue>> getAll(SpartanPlayer player,
                                                       long situation,
                                                       PatternGeneralization patternGeneralization) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + patternGeneralization.generalization;

        if (DetectionNotifications.isEnabled(player)) {
            Collection<Collection<PatternValue>> result = this.getAllRaw(situation, patternGeneralization);

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
                    Collection<Collection<PatternValue>> result = this.getAllRaw(situation, patternGeneralization);
                    allCache.put(hash, new PatternAllCache(result));
                    return result;
                } else if (cache.isExpired()) {
                    return cache.update(this.getAllRaw(situation, patternGeneralization));
                } else {
                    return cache.data;
                }
            }
        }
    }

    private Collection<Collection<PatternValue>> getAllRaw(long situation,
                                                           PatternGeneralization patternGeneralization) {
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
                            Collection<PatternValue> data = entry.getValue().correlationsData(patternGeneralization).get(situation);

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

    public List<PatternValue> getSpecific(PlayerProfile profile,
                                          long situation,
                                          PatternGeneralization patternGeneralization,
                                          int count) {
        synchronized (this.profiles) {
            PatternProfile patternProfile = this.profiles.get(profile);

            if (patternProfile == null) {
                return new ArrayList<>(0);
            } else {
                synchronized (patternProfile.correlations) {
                    List<PatternValue> specific = patternProfile.correlationsData(patternGeneralization).get(situation);

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

    public ProbabilityRank getRanked(SpartanPlayer player,
                                     long situation,
                                     PatternGeneralization patternGeneralization) {
        long hash = (Long.hashCode(situation) * SpartanBukkit.hashCodeMultiplierLong)
                + patternGeneralization.generalization;

        if (DetectionNotifications.isEnabled(player)) {
            ProbabilityRank result = this.getRankedRaw(situation, patternGeneralization);

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
                    cache = new PatternRankCache(this.getRankedRaw(situation, patternGeneralization));
                    rankedCache.put(
                            hash,
                            cache
                    );
                    return cache.data;
                } else if (cache.isExpired()) {
                    return cache.update(this.getRankedRaw(situation, patternGeneralization));
                } else {
                    return cache.data;
                }
            }
        }
    }

    public ProbabilityRank getRankedRaw(long situation, PatternGeneralization patternGeneralization) {
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
                            Collection<PatternValue> data = entry.getValue().correlationsData(patternGeneralization).get(situation);

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

    public double getSignificance(Number number, PatternGeneralization patternGeneralization) {
        synchronized (this.patternProbability) {
            if (this.patternProbability.isExpired()) {
                this.patternProbability.data.clear();

                synchronized (this.profiles) {
                    for (Map.Entry<PlayerProfile, PatternProfile> entry : this.profiles.entrySet()) {
                        if (entry.getKey().isLegitimate()) {
                            synchronized (entry.getValue().correlations) {
                                Map<Long, List<PatternValue>> data = entry.getValue().correlationsData(patternGeneralization);

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

    public void calculate(SpartanPlayer player,
                          PatternGeneralization patternGeneralization,
                          long contribution,
                          long time) {
        boolean include = include(player);

        synchronized (this.profiles) {
            PatternProfile patternProfile = getProfileRaw(player.getProfile());

            synchronized (patternProfile.contribution) {
                patternProfile.contributionData(patternGeneralization).add(contribution);

                if (include) {
                    synchronized (patternGeneralization.contributionRank) {
                        patternGeneralization.contributionRank.put(
                                patternProfile,
                                patternGeneralization,
                                patternProfile.contributionRaw(patternGeneralization),
                                time,
                                true
                        );
                    }
                }
            }
        }
    }

    public void calculate(SpartanPlayer player,
                          PatternGeneralization patternGeneralization,
                          double probability, double significance,
                          long time) {
        boolean include = include(player);

        synchronized (this.profiles) {
            PatternProfile patternProfile = getProfileRaw(player.getProfile());

            synchronized (patternProfile.probability) {
                patternProfile.total[PatternProfile.PROBABILITY_POSITION] += probability;
                patternProfile.probabilityData(patternGeneralization).add(probability);

                if (include) {
                    synchronized (patternGeneralization.probabilityRank) {
                        patternGeneralization.probabilityRank.put(
                                patternProfile,
                                patternGeneralization,
                                patternProfile.probabilityRaw(patternGeneralization),
                                time,
                                true
                        );
                    }
                }
            }
            synchronized (patternProfile.significance) {
                patternProfile.total[PatternProfile.SIGNIFICANCE_POSITION] += significance;
                patternProfile.significanceData(patternGeneralization).add(significance);

                if (include) {
                    synchronized (patternGeneralization.significanceRank) {
                        patternGeneralization.significanceRank.put(
                                patternProfile,
                                patternGeneralization,
                                patternProfile.significanceRaw(patternGeneralization),
                                time,
                                true
                        );
                    }
                }
            }
        }
    }

    // Separator

    private PatternProfile getProfileRaw(PlayerProfile profile) {
        return this.profiles.computeIfAbsent(profile, k -> new PatternProfile(this, profile));
    }

    public PatternProfile getProfile(PlayerProfile profile) {
        synchronized (this.profiles) {
            return this.getProfileRaw(profile);
        }
    }
}
