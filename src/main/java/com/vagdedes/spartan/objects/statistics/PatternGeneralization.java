package com.vagdedes.spartan.objects.statistics;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;

public class PatternGeneralization {

    static String path(String key, int generalization) {
        return Register.plugin.getDataFolder()
                + "/learning"
                + "/" + key
                + "/" + generalization;
    }

    static final String
            profileOption = ".profile",
            patternOption = ".pattern";

    // Separator

    final String key;
    final int generalization;
    private int count;
    private File file;
    private YamlConfiguration configuration;

    PatternGeneralization(String key, int generalization) {
        this.key = key;
        this.generalization = generalization;
        this.count = 0;
        this.reload();
    }

    private void reload() {
        int hash = Objects.hash(this.key, this.generalization, System.currentTimeMillis());
        this.file = new File(path(key, generalization) + "/" + hash + ".yml");

        if (this.file.exists()) {
            this.reload();
            return;
        } else {
            try {
                this.file.createNewFile();
            } catch (Exception ignored) {
            }
        }
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
    }

    void set(PatternValue value) {
        SpartanBukkit.dataThread.execute(() -> {
            this.count++;
            this.configuration.set(value.time + patternOption, value.number);
            this.configuration.set(value.time + profileOption, value.profile);
        });
    }

    void store() {
        SpartanBukkit.dataThread.execute(() -> {
            try {
                this.configuration.save(this.file);

                if (this.count >= 10_000) {
                    this.count = 0;
                    this.reload();
                }
            } catch (Exception ignored) {
            }
        });
    }
}
