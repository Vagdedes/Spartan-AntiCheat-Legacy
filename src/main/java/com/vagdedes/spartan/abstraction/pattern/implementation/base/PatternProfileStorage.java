package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.pattern.PatternGeneralization;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternProfileStorage {

    private static String headPath(PatternStorage patternStorage, PatternProfile patternProfile) {
        return Register.plugin.getDataFolder()
                + "/comparison/"
                + patternStorage.key + "/"
                + patternProfile.profile.getName();
    }

    static final class GeneralizationFile {

        private final PatternProfileStorage parent;
        private final int generalization;
        private File file;
        private YamlConfiguration configuration;
        private byte store;
        private short count;

        private GeneralizationFile(PatternProfileStorage patternProfileStorage,
                                   PatternGeneralization patternGeneralization) {
            this.parent = patternProfileStorage;
            this.generalization = patternGeneralization.generalization;
            this.count = 0;
            this.store = 0;
            this.reload();
        }

        private void set(long time, String key, Object value) {
            SpartanBukkit.dataThread.execute(() -> {
                this.count++;
                this.configuration.set(time + "." + key, value);

                if (this.store == 0) {
                    this.store = 1;

                    synchronized (this.parent.profile.parent.profileFiles) {
                        this.parent.profile.parent.profileFiles.add(this);
                    }
                }
            });
        }

        private void reload() {
            int hash = Objects.hash(
                    this.parent.profile.parent.key,
                    this.generalization,
                    System.currentTimeMillis()
            );
            this.file = new File(
                    headPath(this.parent.profile.parent, this.parent.profile) + "/"
                            + this.generalization + "/"
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
    }

    private final PatternProfile profile;
    private final Map<Integer, GeneralizationFile> files;

    PatternProfileStorage(PatternProfile patternProfile) {
        this.profile = patternProfile;
        this.files = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    void set(PatternGeneralization patternGeneralization, long time, String key, Object value) {
        synchronized (this.files) {
            this.files.computeIfAbsent(
                    patternGeneralization.generalization,
                    integer -> new GeneralizationFile(this, patternGeneralization)
            ).set(time, key, value);
        }
    }

    void delete() {
        synchronized (this.files) {
            synchronized (this.profile.parent.profileFiles) {
                Iterator<PatternProfileStorage.GeneralizationFile> iterator = this.profile.parent.profileFiles.iterator();

                while (iterator.hasNext()) {
                    PatternProfileStorage.GeneralizationFile generalizationFile = iterator.next();
                    this.profile.parent.profileFiles.remove(generalizationFile);
                    iterator.remove();
                    SpartanBukkit.dataThread.execute(() -> generalizationFile.file.delete());
                }
            }
            File file = new File(headPath(this.profile.parent, this.profile));

            if (file.exists() && file.isDirectory()) {
                this.deleteFiles(file);
            }
        }
    }

    private void deleteFiles(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();

            if (files != null) {
                for (File subFile : files) {
                    this.deleteFiles(subFile);
                }
            }
        }
        file.delete();
    }
}
