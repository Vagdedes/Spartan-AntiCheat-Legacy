package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

public class PatternGeneralizationFile {

    private static String headPath(PatternProfile patternProfile) {
        return Register.plugin.getDataFolder()
                + "/comparison/"
                + patternProfile.patternGeneralization.parent.key + "/"
                + patternProfile.researchProfile.getName();
    }

    private final PatternProfile profile;
    private File file;
    private YamlConfiguration configuration;
    private byte store;
    private short count;

    PatternGeneralizationFile(PatternProfile patternProfile) {
        this.profile = patternProfile;
        this.count = 0;
        this.store = 0;
        this.reload();
    }

    void set(long time, String key, Object value) {
        SpartanBukkit.dataThread.execute(() -> {
            this.count++;
            this.configuration.set(time + "." + key, value);

            if (this.store == 0) {
                this.store = 1;

                synchronized (this.profile.patternGeneralization.parent.profileFiles) {
                    this.profile.patternGeneralization.parent.profileFiles.add(this);
                }
            }
        });
    }

    private void reload() {
        int hash = Objects.hash(
                this.profile.patternGeneralization.parent.key,
                this.profile.patternGeneralization.generalization,
                System.currentTimeMillis()
        );
        this.file = new File(
                headPath(this.profile) + "/"
                        + this.profile.patternGeneralization.generalization + "/"
                        + hash + ".yml"
        );

        if (this.file.exists()) {
            this.reload();
        } else {
            this.configuration = YamlConfiguration.loadConfiguration(this.file);
        }
    }

    void store() {
        if (this.store == 1) {
            this.store = 2;

            SpartanBukkit.dataThread.execute(() -> {
                try {
                    this.configuration.save(this.file);
                    this.store = 0;

                    if (this.count < 0) { // Short has overflowed (32767)
                        this.count = 0;
                        this.reload();
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    void load() { // todo
        SpartanBukkit.dataThread.execute(() -> {
            File file = new File(
                    headPath(this.profile)
                            + "/" + this.profile.patternGeneralization.generalization
            );

            if (file.exists() && file.isDirectory()) {
                File[] files = file.listFiles();

                if (files != null) {
                    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

                    for (int i = 0; i < Math.min(files.length, 7); i++) { // 7 is less than 1% chance
                        File subFile = files[i];

                        if (subFile.isFile() && subFile.getName().endsWith(".yml")) {
                            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(subFile);
                            Set<String> keys = configuration.getKeys(false);

                            if (!keys.isEmpty()) {
                                for (String key : keys) {
                                    try {
                                        for (PatternComparison patternComparison : this.profile.patternGeneralization.patternComparisons) {
                                            double value = configuration.getDouble(key + "." + patternComparison.key, -1.0);

                                            if (value != -1.0) {
                                                patternComparison.put(
                                                        this.profile,
                                                        value,
                                                        Long.parseLong(key),
                                                        false
                                                );
                                            }
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    void delete() {
        this.file.delete();
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
