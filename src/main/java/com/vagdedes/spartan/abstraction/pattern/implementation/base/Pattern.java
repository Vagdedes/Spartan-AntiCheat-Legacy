package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Pattern {

    private static final Collection<Pattern> instances = Collections.synchronizedList(new ArrayList<>());

    public static void delete(PlayerProfile profile) {
        synchronized (instances) {
            for (Pattern instance : instances) {
                synchronized (instance.generalizations) {
                    for (PatternGeneralization patternGeneralization : instance.generalizations.values()) {
                        synchronized (patternGeneralization.profiles) {
                            PatternProfile patternProfile = patternGeneralization.profiles.remove(profile);

                            if (patternProfile != null) {
                                patternProfile.storage.delete();
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
            synchronized (PatternGeneralization.allCache) {
                synchronized (PatternGeneralization.rankedCache) {
                    instances.clear();
                    PatternGeneralization.allCache.clear();
                    PatternGeneralization.rankedCache.clear();
                }
            }
        }
    }

    // Separator

    public final String key;
    final String learningFolder;
    private final Map<Integer, PatternGeneralization> generalizations;
    private final Set<PatternProfileStorage> profileFiles;
    private boolean load;

    public Pattern(String key, int[] generalizations) {
        this.key = key;
        this.learningFolder = Register.plugin.getDataFolder()
                + "/learning/"
                + key;
        this.load = false;
        this.profileFiles = Collections.synchronizedSet(new HashSet<>());

        this.generalizations = Collections.synchronizedMap(
                new LinkedHashMap<>(generalizations.length + 1)
        );
        this.generalizations.put(0, new PatternGeneralizationStorage(this));

        if (generalizations.length > 0) {
            for (int generalization : generalizations) {
                this.generalizations.put(generalization, new PatternGeneralizationMemory(this, generalization));
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
                Iterator<PatternProfileStorage> iterator = this.profileFiles.iterator();

                while (iterator.hasNext()) {
                    iterator.next().store();
                    iterator.remove();
                }
            }
        }
    }

    void queueFile(PatternProfileStorage file) {
        synchronized (this.profileFiles) {
            this.profileFiles.add(file);
        }
    }

    private void load() {
        if (this.load) {
            return;
        }
        this.load = true;
        store();
        Map<Integer, PatternGeneralization> generalizations = new LinkedHashMap<>();
        File directory = new File(this.learningFolder);

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
                                    ? new PatternGeneralizationStorage(this)
                                    : new PatternGeneralizationMemory(this, entry.getKey())
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

                                                synchronized (this.generalizations) {
                                                    for (PatternGeneralization generalization : generalizations.values()) {
                                                        generalization.rawCache(
                                                                profile,
                                                                new PatternValue(
                                                                        situation,
                                                                        (Number) pattern,
                                                                        generalization,
                                                                        time
                                                                ),
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
        if (!generalizations.isEmpty()) {
            for (PatternGeneralization patternGeneralization : generalizations.values()) {
                // todo
            }
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

    public void learn(SpartanPlayer player, long situation, Number number, boolean collect) {
        if (collect) {
            long time = System.currentTimeMillis();
            Integer divisor = DetectionNotifications.getDivisor(player, false);
            boolean include = divisor == null || divisor != 69;

            synchronized (this.generalizations) {
                for (PatternGeneralization generalization : this.generalizations.values()) {
                    generalization.rawCache(
                            player.getProfile(),
                            new PatternValue(
                                    situation,
                                    number,
                                    generalization,
                                    time
                            ),
                            include,
                            include && generalization instanceof PatternGeneralizationStorage
                    );
                }
            }
        }
    }

    // Separator

    public PatternGeneralization getGeneralization(int generalization) {
        synchronized (this.generalizations) {
            return this.generalizations.get(generalization);
        }
    }
}
