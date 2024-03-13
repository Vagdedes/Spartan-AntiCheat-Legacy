package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.utils.java.HashHelper;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternStorage {

    private static final Collection<PatternStorage> instances = Collections.synchronizedList(new ArrayList<>());

    public static void consider(PlayerProfile profile) {
        synchronized (instances) {
            for (PatternStorage instance : instances) {
                instance.objectConsider(profile);
            }
        }
    }

    public static void block(PlayerProfile profile) {
        synchronized (instances) {
            for (PatternStorage instance : instances) {
                instance.objectBlock(profile);
            }
        }
    }

    public static void delete(PlayerProfile profile) {
        synchronized (instances) {
            for (PatternStorage instance : instances) {
                synchronized (instance.blocked) {
                    synchronized (instance.correlations) {
                        Map<Integer, Collection<PatternValue>> map = instance.correlations.get(profile);

                        if (map != null) {
                            Runnable delete = () -> {
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
                                                                configuration.set(key + PatternGeneralization.profileOption, null);
                                                                configuration.set(key + PatternGeneralization.patternOption, null);
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
                            };

                            if (instance.blocked.remove(profile)) {
                                synchronized (instance.unavailable) {
                                    for (Map.Entry<Integer, Collection<PatternValue>> entry : map.entrySet()) {
                                        instance.unavailable.get(entry.getKey()).removeAll(entry.getValue());
                                    }
                                    delete.run();
                                }
                            } else {
                                synchronized (instance.available) {
                                    for (Map.Entry<Integer, Collection<PatternValue>> entry : map.entrySet()) {
                                        instance.available.get(entry.getKey()).removeAll(entry.getValue());
                                    }
                                    delete.run();
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
                patternStorage.objectReload();
            }
        }
    }

    // Separator

    public final String key;
    private final PatternGeneralization[] generalizations;
    private final Map<Integer, Collection<PatternValue>> available, unavailable;
    private final Map<PlayerProfile, Map<Integer, Collection<PatternValue>>> correlations;
    private final Set<PlayerProfile> blocked;
    private boolean store;

    public PatternStorage(String key, int[] generalizations) {
        this.key = key;
        this.store = false;

        this.blocked = Collections.synchronizedSet(new HashSet<>());
        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.available = Collections.synchronizedMap(new LinkedHashMap<>());
        this.unavailable = Collections.synchronizedMap(new LinkedHashMap<>());

        this.generalizations = new PatternGeneralization[generalizations.length + 1];
        this.generalizations[0] = new PatternGeneralizationStorage(key);

        if (generalizations.length > 0) {
            for (int count = 0; count < generalizations.length; count++) {
                this.generalizations[count + 1] = new PatternGeneralizationMemory(key, generalizations[count]);
            }
        }

        for (PatternGeneralization generalization : this.generalizations) {
            this.available.put(generalization.generalization, new ArrayList<>());
            this.unavailable.put(generalization.generalization, new ArrayList<>());
        }

        synchronized (instances) {
            instances.add(this);
        }
    }

    // Separator

    public void store() {
        if (this.store) {
            this.store = false;
            ((PatternGeneralizationStorage) this.generalizations[0]).store();
        }
    }

    private void objectReload() {
        store();
        Map<Integer, Collection<PatternValue>>
                available = new LinkedHashMap<>(),
                unavailable = new LinkedHashMap<>();
        Map<PlayerProfile, Map<Integer, Collection<PatternValue>>> correlations = new LinkedHashMap<>();
        File directory = new File(PatternGeneralization.path(this.key));

        if (!directory.exists()) {
            directory.mkdirs();
        } else if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
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
                                                            profile.isLegitimate() ? available : unavailable,
                                                            correlations,
                                                            null,
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
            synchronized (this.available) {
                synchronized (this.unavailable) {
                    this.correlations.clear();
                    this.correlations.putAll(correlations);

                    for (Collection<PatternValue> values : this.available.values()) {
                        values.clear();
                    }
                    this.available.putAll(available);

                    for (Collection<PatternValue> values : this.unavailable.values()) {
                        values.clear();
                    }
                    this.unavailable.putAll(unavailable);
                }
            }
        }
    }

    // Separator

    private void rawCache(Map<Integer, Collection<PatternValue>> map,
                          Map<PlayerProfile, Map<Integer, Collection<PatternValue>>> correlations,
                          SpartanPlayer player,
                          PlayerProfile profile,
                          PatternValue value,
                          PatternGeneralization generalization,
                          boolean set) {
        map.get(generalization.generalization).add(value);
        correlations.computeIfAbsent(
                profile,
                k -> new LinkedHashMap<>()
        ).computeIfAbsent(
                generalization.generalization,
                k -> new ArrayList<>()
        ).add(value);

        if (set) {
            ((PatternGeneralizationStorage) generalization).set(profile, value);
            this.store = true;
        }
    }

    public void collect(SpartanPlayer player, Number number, boolean collect) {
        if (collect) {
            long time = System.currentTimeMillis();
            boolean legitimate = player.getProfile().isLegitimate();

            synchronized (legitimate ? this.available : this.unavailable) {
                synchronized (this.correlations) {
                    for (PatternGeneralization generalization : this.generalizations) {
                        this.rawCache(
                                legitimate ? this.available : this.unavailable,
                                this.correlations,
                                player,
                                player.getProfile(),
                                new PatternValue(
                                        HashHelper.hashPlayer(player),
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
    }

    public void collect(SpartanPlayer player, Number number) {
        collect(player, number, true);
    }

    // Separator

    private void objectConsider(PlayerProfile profile) {
        synchronized (this.blocked) {
            if (this.blocked.contains(profile)) {
                this.blocked.remove(profile);
                Map<Integer, Collection<PatternValue>> map = this.correlations.get(profile);

                if (map != null) {
                    for (Map.Entry<Integer, Collection<PatternValue>> entry : map.entrySet()) {
                        synchronized (this.available) {
                            synchronized (this.unavailable) {
                                this.unavailable.get(entry.getKey()).removeAll(entry.getValue());
                                this.available.get(entry.getKey()).addAll(entry.getValue());
                            }
                        }
                    }
                }
            }
        }
    }

    private void objectBlock(PlayerProfile profile) {
        synchronized (this.blocked) {
            if (!this.blocked.contains(profile)) {
                this.blocked.add(profile);
                Map<Integer, Collection<PatternValue>> map = this.correlations.get(profile);

                if (map != null) {
                    for (Map.Entry<Integer, Collection<PatternValue>> entry : map.entrySet()) {
                        synchronized (this.available) {
                            synchronized (this.unavailable) {
                                this.available.get(entry.getKey()).removeAll(entry.getValue());
                                this.unavailable.get(entry.getKey()).addAll(entry.getValue());
                            }
                        }
                    }
                }
            }
        }
    }

    // Separator

    public Collection<PatternValue> get() {
        return this.get(0);
    }

    public Collection<PatternValue> get(int generalization) {
        synchronized (this.available) {
            return this.available.get(generalization);
        }
    }

    public Collection<PatternValue> get(SpartanPlayer player, int count) {
        return this.get(player, 0, count);
    }

    public Collection<PatternValue> get(SpartanPlayer player, int generalization, int count) {
        synchronized (this.correlations) {
            Map<Integer, Collection<PatternValue>> map = this.correlations.get(player.getProfile());

            if (map != null) {
                Collection<PatternValue> values = map.get(generalization);

                if (values != null) {
                    int size = values.size();

                    if (size > count) {
                        size--;
                        return new ArrayList<>(values).subList(size - count, size);
                    }
                }
            }
            return new ArrayList<>(0);
        }
    }
}
