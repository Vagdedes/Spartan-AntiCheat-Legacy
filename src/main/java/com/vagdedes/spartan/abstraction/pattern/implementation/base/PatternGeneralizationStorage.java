package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class PatternGeneralizationStorage extends PatternGeneralization {

    private static final int GB = 1024 * 1024 * 1024;

    private short count;
    private File file;
    private YamlConfiguration configuration;

    PatternGeneralizationStorage(Pattern pattern, PatternGeneralizationInitiator initiator) {
        super(pattern, initiator);
        this.count = 0;
        this.reload();
    }

    private void reload() {
        File[] files = this.parent.getLearningFiles();

        if (files != null) {
            long length = 0;

            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                if (file.isFile() && file.getName().endsWith(".yml")) {
                    length += file.length();
                } else {
                    files[i] = null;
                }
            }

            if (length >= GB) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));

                for (File file : files) {
                    if (file != null) {
                        long fileLength = file.length();

                        if (file.delete()) {
                            length -= fileLength;

                            if (length < GB) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        int hash = Objects.hash(this.parent.key, this.initiator.generalization, System.currentTimeMillis());
        this.file = new File(this.parent.learningFolder + "/" + hash + ".yml");

        if (this.file.exists()) {
            this.reload();
        } else {
            this.configuration = YamlConfiguration.loadConfiguration(this.file);
        }
    }

    void set(PlayerProfile profile, long time, long situation, float pattern) {
        SpartanBukkit.dataThread.execute(() -> {
            this.count++;
            this.configuration.set(time + profileOption, profile.getName());
            this.configuration.set(time + situationOption, situation);
            this.configuration.set(time + patternOption, pattern);
        });
    }

    void store() {
        SpartanBukkit.dataThread.execute(() -> {
            try {
                this.configuration.save(this.file);

                if (this.count < 0) { // Short has overflowed (32767)
                    this.count = 0;
                    this.reload();
                }
            } catch (Exception ignored) {
            }
        });
    }
}
