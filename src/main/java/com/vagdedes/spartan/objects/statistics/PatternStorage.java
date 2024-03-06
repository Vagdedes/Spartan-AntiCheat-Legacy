package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternStorage {

    static final String directoryPath = Register.plugin.getDataFolder() + "/learning";

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
                            if (instance.blocked.remove(profile)) {
                                synchronized (instance.unavailable) {
                                    for (Map.Entry<Integer, Collection<PatternValue>> entry : map.entrySet()) {
                                        instance.unavailable.get(entry.getKey()).removeAll(entry.getValue());
                                    }
                                }
                            } else {
                                synchronized (instance.available) {
                                    for (Map.Entry<Integer, Collection<PatternValue>> entry : map.entrySet()) {
                                        instance.available.get(entry.getKey()).removeAll(entry.getValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Separator

    public final String key;
    private final PatternGeneralization[] generalizations;
    private final Map<Integer, Collection<PatternValue>> available, unavailable;
    private final Map<PlayerProfile, Map<Integer, Collection<PatternValue>>> correlations;
    private final Set<PlayerProfile> blocked;

    public PatternStorage(String key, int[] generalizations) {
        this.key = key;
        this.blocked = Collections.synchronizedSet(new HashSet<>());

        this.generalizations = new PatternGeneralization[generalizations.length + 1];
        this.generalizations[0] = new PatternGeneralization(key, 0);

        if (generalizations.length > 0) {
            for (int count = 0; count < generalizations.length; count++) {
                this.generalizations[count + 1] = new PatternGeneralization(key, generalizations[count]);
            }
        }

        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());

        this.available = Collections.synchronizedMap(new LinkedHashMap<>());
        this.available.put(0, new ArrayList<>());

        this.unavailable = Collections.synchronizedMap(new LinkedHashMap<>());
        this.unavailable.put(0, new ArrayList<>());

        for (int generalization : generalizations) {
            this.available.put(generalization, new ArrayList<>());
            this.unavailable.put(generalization, new ArrayList<>());
        }
        synchronized (instances) {
            instances.add(this);
        }
    }

    // Separator

    public void store() {
        for (PatternGeneralization generalization : this.generalizations) {
            generalization.store();
        }
    }

    public void reload() {
        store();
        Map<Integer, Collection<PatternValue>>
                available = new LinkedHashMap<>(),
                unavailable = new LinkedHashMap<>();
        Map<PlayerProfile, Map<Integer, Collection<PatternValue>>> correlations = new LinkedHashMap<>();
        File directory = new File(directoryPath + "/" + this.key);

        if (!directory.exists()) {
            directory.mkdirs();
        } else if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".yml")) {
                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                        Set<String> keys = configuration.getKeys(false);

                        if (!keys.isEmpty()) {
                            for (String key : keys) {
                                try {
                                    long time = Long.parseLong(key);
                                    Object pattern = configuration.get(key + ".pattern", null);

                                    if (pattern instanceof Number) {
                                        String profileName = configuration.getString(key + ".profile", null);

                                        if (profileName != null && !profileName.isEmpty()) {
                                            Number number = (Number) pattern;
                                            PlayerProfile profile = ResearchEngine.getPlayerProfile(profileName);
                                            Map<Integer, Collection<PatternValue>> map =
                                                    profile.isLegitimate() ? available : unavailable;

                                            for (PatternGeneralization generalization : this.generalizations) {
                                                this.rawCache(
                                                        map,
                                                        correlations,
                                                        profile,
                                                        new PatternValue(
                                                                profile,
                                                                number,
                                                                generalization,
                                                                time
                                                        ),
                                                        generalization,
                                                        false
                                                );
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }
        } else {
            directory.delete();
            directory.mkdirs();
        }

        synchronized (this.correlations) {
            synchronized (this.available) {
                synchronized (this.unavailable) {
                    this.correlations.clear();
                    this.correlations.putAll(correlations);

                    this.available.clear();
                    this.available.putAll(available);

                    this.unavailable.clear();
                    this.unavailable.putAll(unavailable);
                }
            }
        }
    }

    // Separator

    private void rawCache(Map<Integer, Collection<PatternValue>> map,
                          Map<PlayerProfile, Map<Integer, Collection<PatternValue>>> correlations,
                          PlayerProfile profile, PatternValue value,
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
            generalization.set(value);
        }
    }

    public void cache(SpartanPlayer player, Number number) {
        long time = System.currentTimeMillis();
        boolean legitimate = player.getProfile().isLegitimate();

        synchronized (legitimate ? this.available : this.unavailable) {
            synchronized (this.correlations) {
                Map<Integer, Collection<PatternValue>> map =
                        legitimate ? this.available : this.unavailable;

                for (PatternGeneralization generalization : this.generalizations) {
                    this.rawCache(
                            map,
                            this.correlations,
                            player.getProfile(),
                            new PatternValue(
                                    player.getProfile(),
                                    number,
                                    generalization,
                                    time
                            ),
                            generalization,
                            true
                    );
                }
            }
        }
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
