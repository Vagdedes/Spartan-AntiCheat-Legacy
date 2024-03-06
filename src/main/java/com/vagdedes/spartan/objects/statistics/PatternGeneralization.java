package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PatternGeneralization {

    final int generalization;
    private final File file;
    private final YamlConfiguration configuration;

    PatternGeneralization(String key, int generalization) {
        this.generalization = generalization;
        this.file = new File(
                PatternStorage.directoryPath
                        + "/" + key
                        + "/" + generalization
                        + "" // todo
        );

        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            } catch (Exception ignored) {
            }
        } else if (!this.file.isFile()) {
            this.file.delete();

            try {
                this.file.createNewFile();
            } catch (Exception ignored) {
            }
        }
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
    }

    void set(PatternValue value) {
        this.configuration.set(value.time + ".pattern", value.number);
        this.configuration.set(value.time + ".profile", value.profile);
    }

    void store() {
        SpartanBukkit.dataThread.execute(() -> {
            try {
                this.configuration.save(this.file);
            } catch (Exception ignored) {
            }
        });
    }
}
