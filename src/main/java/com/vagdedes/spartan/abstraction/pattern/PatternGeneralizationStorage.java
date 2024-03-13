package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.system.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;

public class PatternGeneralizationStorage extends PatternGeneralization {

    private int count;
    private File file;
    private YamlConfiguration configuration;

    PatternGeneralizationStorage(String key, int generalization) {
        super(key, generalization);
        this.count = 0;

        if (generalization == 0) {
            this.reload();
        }
    }

    private void reload() {
        int hash = Objects.hash(this.key, this.generalization, System.currentTimeMillis());
        this.file = new File(path(key) + "/" + hash + ".yml");

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

    void set(PlayerProfile profile, PatternValue value) {
        SpartanBukkit.dataThread.execute(() -> {
            this.count++;
            this.configuration.set(value.time + patternOption, value.number);
            this.configuration.set(value.time + profileOption, profile);
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
