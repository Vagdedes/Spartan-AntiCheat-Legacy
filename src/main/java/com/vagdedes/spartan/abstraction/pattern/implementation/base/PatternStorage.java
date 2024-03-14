package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternStorage {

    private static final boolean debug = true;
    private static final Collection<PatternStorage> instances = Collections.synchronizedList(new ArrayList<>());

    public static void delete(PlayerProfile profile) {
        synchronized (instances) {
            for (PatternStorage instance : instances) {
                synchronized (instance.correlations) {
                    if (instance.correlations.remove(profile) != null) {
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

    // Separator

    public final String key;
    private final PatternGeneralization[] generalizations;
    // Profile -> Generalization -> Situation -> Patterns
    private final Map<PlayerProfile, Map<Integer, Map<Long, LinkedList<PatternValue>>>> correlations;

    public PatternStorage(String key, int[] generalizations) {
        this.key = key;

        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());

        this.generalizations = new PatternGeneralization[generalizations.length + 1];
        this.generalizations[0] = new PatternGeneralizationStorage(key);

        if (generalizations.length > 0) {
            for (int count = 0; count < generalizations.length; count++) {
                this.generalizations[count + 1] = new PatternGeneralizationMemory(key, generalizations[count]);
            }
        }

        synchronized (instances) {
            instances.add(this);
        }
    }

    // Separator

    public void store() {
        ((PatternGeneralizationStorage) this.generalizations[0]).store();
    }

    private void load() {
        store();
        Map<PlayerProfile, Map<Integer, Map<Long, LinkedList<PatternValue>>>> correlations = new LinkedHashMap<>();
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

                                                for (PatternGeneralization generalization : this.generalizations) {
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
                directory.delete();
                directory.mkdirs();
            }
        }

        synchronized (this.correlations) {
            this.correlations.clear();
            this.correlations.putAll(correlations);
        }
    }

    // Separator

    private void rawCache(Map<PlayerProfile, Map<Integer, Map<Long, LinkedList<PatternValue>>>> correlations,
                          PlayerProfile profile,
                          PatternValue value,
                          PatternGeneralization generalization,
                          boolean set) {
        LinkedList<PatternValue> list = correlations.computeIfAbsent(
                profile,
                k -> new LinkedHashMap<>()
        ).computeIfAbsent(
                generalization.generalization,
                k -> new LinkedHashMap<>()
        ).computeIfAbsent(
                value.situation,
                k -> new LinkedList<>() // todo
        );
        list.add(value);

        if (set) {
            ((PatternGeneralizationStorage) generalization).set(profile, value);
        }
    }

    public void collect(PlayerProfile profile, long situation, Number number, boolean collect) {
        if (collect) {
            long time = System.currentTimeMillis();

            synchronized (this.correlations) {
                for (PatternGeneralization generalization : this.generalizations) {
                    this.rawCache(
                            this.correlations,
                            profile,
                            new PatternValue(
                                    situation,
                                    number,
                                    generalization,
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

    public Collection<Collection<PatternValue>> getAll(PlayerProfile dismiss, long situation, int generalization) {
        Collection<Collection<PatternValue>> collections = new ArrayList<>();

        synchronized (this.correlations) {
            for (Map.Entry<PlayerProfile, Map<Integer, Map<Long, LinkedList<PatternValue>>>> entry : this.correlations.entrySet()) {
                if (entry.getKey().isLegitimate()
                        && (debug
                        || !entry.getKey().equals(dismiss))) {
                    Collection<PatternValue> situations = entry.getValue().get(generalization).get(situation);

                    if (situations != null) {
                        collections.add(situations);
                    }
                }
            }
        }
        return !debug && collections.size() < ResearchEngine.profileRequirement
                ? new ArrayList<>(0)
                : collections;
    }

    public List<PatternValue> getSpecific(PlayerProfile profile, long situation, int generalization, int count) {
        synchronized (this.correlations) {
            Map<Integer, Map<Long, LinkedList<PatternValue>>> map = this.correlations.get(profile);

            if (map == null) {
                return new ArrayList<>(0);
            } else {
                List<PatternValue> specific = map.get(generalization).get(situation);

                if (specific == null) {
                    return new ArrayList<>(0);
                } else {
                    return this.subList(specific, count);
                }
            }
        }
    }

    public List<PatternValue> subList(List<PatternValue> specific, int count) {
        int size = specific.size();

        if (size < count) {
            return new ArrayList<>(0);
        } else {
            return specific.subList(size - count, size - 1);
        }
    }
}
