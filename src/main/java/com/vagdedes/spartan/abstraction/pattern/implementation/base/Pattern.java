package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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

    private static int dataLimit() {
        return SpartanEdition.is2ndGeneration() ? 1_000_000 : 100_000;
    }

    // Separator

    public final String key;
    private final Map<Integer, PatternGeneralization> generalizations;
    final Map<PlayerProfile, PatternProfile> profiles;
    final Set<PatternProfileStorage.GeneralizationFile> profileFiles;
    private final PatternCache patternSizeCache;
    private boolean load;

    public Pattern(String key, int[] generalizations) {
        this.key = key;
        this.load = false;
        this.patternSizeCache = new PatternCache();
        this.profiles = Collections.synchronizedMap(new LinkedHashMap<>());
        this.profileFiles = Collections.synchronizedSet(new HashSet<>());

        this.generalizations = Collections.synchronizedMap(
                new LinkedHashMap<>(generalizations.length + 1)
        );
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
        synchronized (this.generalizations) {
            ((PatternGeneralizationStorage) this.generalizations.get(0)).store();
        }

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
        if (this.load) {
            return;
        }
        this.load = true;
        store();
        Map<PlayerProfile, PatternProfile> correlations = new LinkedHashMap<>();
        Map<Integer, PatternGeneralization> generalizations = new LinkedHashMap<>();
        File directory = new File(PatternGeneralization.path(this.key));

        if (!directory.exists()) {
            SpartanBukkit.dataThread.execute(() -> directory.mkdirs());
        } else if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                Set<Map.Entry<Integer, PatternGeneralization>> entries;

                synchronized (this.generalizations) {
                    entries = new HashSet<>(this.generalizations.entrySet());
                }

                for (Map.Entry<Integer, PatternGeneralization> entry : entries) {
                    generalizations.put(
                            entry.getKey(),
                            entry.getValue() instanceof PatternGeneralizationStorage
                                    ? new PatternGeneralizationStorage(key)
                                    : new PatternGeneralizationMemory(key, entry.getKey())
                    );
                }
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
                                                PatternProfile patternProfile = this.getProfileRaw(correlations, profile);

                                                synchronized (this.generalizations) {
                                                    for (PatternGeneralization generalization : generalizations.values()) {
                                                        this.rawCache(
                                                                patternProfile,
                                                                new PatternValue(
                                                                        situation,
                                                                        (Number) pattern,
                                                                        generalization,
                                                                        time
                                                                ),
                                                                generalization,
                                                                true,
                                                                false
                                                        );
                                                    }
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

        // Separator
        if (!correlations.isEmpty() && !generalizations.isEmpty()) {
            for (PatternGeneralization patternGeneralization : generalizations.values()) {
                for (PatternProfile patternProfile : correlations.values()) {
                    patternProfile.storage.load(patternGeneralization);
                }
            }
        }

        synchronized (this.profiles) {
            this.profiles.clear();
            this.profiles.putAll(correlations);
        }
        if (!generalizations.isEmpty()) {
            synchronized (this.generalizations) {
                this.generalizations.clear();
                this.generalizations.putAll(generalizations);
            }
        }
        this.load = false;
    }

    // Separator

    private void rawCache(PatternProfile patternProfile,
                          PatternValue value,
                          PatternGeneralization patternGeneralization,
                          boolean significant,
                          boolean store) {
        List<PatternValue> list = patternProfile.correlationsData(
                patternGeneralization
        ).computeIfAbsent(
                value.situation,
                k -> new ArrayList<>(100)
        );
        list.add(value);

        if (significant) {
            patternGeneralization.significancePie.add(value.pattern);
        }

        if (patternSizeCache.isExpired()) {
            patternSizeCache.refresh();
            int dataLimit = dataLimit(),
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
            delete = patternGeneralization.significancePie.getTotal() - dataLimit;

            if (delete > 0) {
                patternGeneralization.significancePie.removeOld(delete);
            }
        }

        if (store) {
            ((PatternGeneralizationStorage) patternGeneralization).set(patternProfile, value);
        }
    }

    public void learn(SpartanPlayer player, long situation, Number number, boolean collect) {
        if (collect) {
            long time = System.currentTimeMillis();
            boolean include = include(player);

            synchronized (this.profiles) {
                PatternProfile patternProfile = this.getProfileRaw(this.profiles, player.getProfile());

                if (patternProfile.canUsePattern(situation, number)) {
                    synchronized (this.generalizations) {
                        for (PatternGeneralization generalization : this.generalizations.values()) {
                            this.rawCache(
                                    patternProfile,
                                    new PatternValue(
                                            situation,
                                            number,
                                            generalization,
                                            time
                                    ),
                                    generalization,
                                    include,
                                    include && generalization instanceof PatternGeneralizationStorage
                            );
                        }
                    }
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

                        Collection<PatternValue> data = entry.getValue().correlationsData(patternGeneralization).get(situation);

                        if (data != null) {
                            collections.add(data);
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

                        Collection<PatternValue> data = entry.getValue().correlationsData(patternGeneralization).get(situation);

                        if (data != null) {
                            rank.addMultiplePatterns(data);
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
        return patternGeneralization.significancePie.getProbability(number);
    }

    public PatternGeneralization getGeneralization(int generalization) {
        synchronized (this.generalizations) {
            return this.generalizations.get(generalization);
        }
    }

    // Separator

    public void compare(SpartanPlayer player,
                        PatternGeneralization patternGeneralization,
                        long contribution,
                        long time) {
        boolean include = include(player);

        synchronized (this.profiles) {
            PatternProfile patternProfile = getProfileRaw(this.profiles, player.getProfile());

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

    public void compare(SpartanPlayer player,
                        PatternGeneralization patternGeneralization,
                        double probability, double significance,
                        long time) {
        boolean include = include(player);

        synchronized (this.profiles) {
            PatternProfile patternProfile = getProfileRaw(this.profiles, player.getProfile());

            synchronized (patternProfile.mutual) {
                patternProfile.mutualData(patternGeneralization).add(probability, significance);

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

    private PatternProfile getProfileRaw(Map<PlayerProfile, PatternProfile> map, PlayerProfile profile) {
        return map.computeIfAbsent(profile, k -> new PatternProfile(this, profile));
    }

    public PatternProfile getProfile(PlayerProfile profile) {
        synchronized (this.profiles) {
            return this.getProfileRaw(this.profiles, profile);
        }
    }
}
