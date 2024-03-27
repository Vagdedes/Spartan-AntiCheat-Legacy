package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternProfileStorage {

    private final PatternProfile profile;
    private final File file;
    private final YamlConfiguration configuration;
    private byte store;
    private final List<Long> keys;
    private boolean loaded;

    PatternProfileStorage(PatternProfile patternProfile) {
        this.profile = patternProfile;
        this.keys = Collections.synchronizedList(new LinkedList<>());
        this.store = 0;
        this.file = new File(
                Register.plugin.getDataFolder()
                        + "/comparison/"
                        + this.profile.patternGeneralization.parent.key + "/"
                        + this.profile.patternGeneralization.initiator.generalization + "/"
                        + this.profile.patternGeneralization.initiator.specificWeight + "/"
                        + this.profile.researchProfile.getName() + ".yml"
        );
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        if (this.file.exists()) {
            if (this.file.isFile()) {
                this.loaded = false;

                SpartanBukkit.dataThread.execute(() -> {
                    Set<String> keys = this.configuration.getKeys(false);

                    if (!keys.isEmpty()) {
                        Set<Long> set = new LinkedHashSet<>(this.profile.patternGeneralization.initiator.generalWeight);
                        boolean changed = false;

                        for (String key : keys) {
                            try {
                                boolean removed = false;

                                for (PatternComparison patternComparison : this.profile.patternGeneralization.patternComparisons) {
                                    String path = key + "." + patternComparison.key;
                                    double value = this.configuration.getDouble(path, -1.0);

                                    if (value != -1.0) {
                                        long time = Long.parseLong(key);

                                        if (set.size() == this.profile.patternGeneralization.initiator.generalWeight) {
                                            this.configuration.set(path, null);
                                            removed = true;
                                            changed = true;
                                        } else {
                                            set.add(time);
                                            patternComparison.put(
                                                    this.profile,
                                                    value,
                                                    time,
                                                    false
                                            );
                                        }
                                    }
                                }

                                if (removed) {
                                    this.configuration.set(key, null);
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        if (!set.isEmpty()) {
                            if (changed) {
                                try {
                                    this.configuration.save(this.file);
                                } catch (Exception ignored) {
                                }
                            }
                            synchronized (this.keys) {
                                this.keys.clear();
                                this.keys.addAll(set);
                            }
                        }
                    }
                    this.loaded = true;
                });
            } else {
                this.file.delete();
                this.loaded = true;
            }
        } else {
            this.loaded = true;
        }
    }

    void set(PatternComparison comparison, long time, Object value) {
        if (this.loaded) {
            SpartanBukkit.dataThread.execute(() -> {
                synchronized (this.keys) {
                    this.keys.add(time);
                }
                this.configuration.set(time + "." + comparison.key, value);

                if (this.store == 0) {
                    this.store = 1;
                    this.profile.patternGeneralization.parent.queueFile(this);
                }
            });
        }
    }

    void store() {
        if (this.store == 1) {
            this.store = 2;

            SpartanBukkit.dataThread.execute(() -> {
                try {
                    Collection<String> keys = new ArrayList<>();

                    synchronized (this.keys) {
                        int delete = this.keys.size() - this.profile.patternGeneralization.initiator.generalWeight;

                        if (delete > 0) {
                            Iterator<Long> iterator = this.keys.iterator();

                            while (iterator.hasNext()) {
                                keys.add(Long.toString(iterator.next()));
                                iterator.remove();

                                if (--delete == 0) {
                                    break;
                                }
                            }
                        }
                    }
                    if (!keys.isEmpty()) {
                        for (String key : keys) {
                            for (PatternComparison patternComparison : this.profile.patternGeneralization.patternComparisons) {
                                this.configuration.set(key + "." + patternComparison.key, null);
                            }
                            this.configuration.set(key, null);
                        }
                    }
                    this.configuration.save(this.file);
                    this.store = 0;
                } catch (Exception ignored) {
                }
            });
        }
    }

    void delete() {
        if (loaded) {
        this.profile.patternGeneralization.parent.dequeueFile(this);
        this.file.delete();

        synchronized (this.keys) {
            this.keys.clear();
        }
        File directory = new File(this.profile.patternGeneralization.parent.learningFolder);

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
                                            && profileName.equals(this.profile.researchProfile.getName())) {
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
