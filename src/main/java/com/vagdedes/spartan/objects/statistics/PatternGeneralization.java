package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PatternGeneralization {

    static String path(String key, int generalization) {
        return Register.plugin.getDataFolder()
                + "/learning"
                + "/" + key
                + "/" + generalization;
    }

    // Separator

    final int generalization;
    private final File file;
    private final YamlConfiguration configuration;

    PatternGeneralization(String key, int generalization) {
        this.generalization = generalization;
        this.file = new File(path(key, generalization) + "/fileName" + ".yml"); // todo

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
