package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PatternProfileStorage {

    private final PatternProfile patternProfile;
    private final PatternStorage patternStorage;
    private short count;
    private File file;
    private YamlConfiguration configuration;
    private byte store;

    PatternProfileStorage(PatternStorage patternStorage, PatternProfile patternProfile) {
        this.patternStorage = patternStorage;
        this.patternProfile = patternProfile;
        this.count = 0;
        this.store = 0;
        this.reload();
    }

    private void reload() {
        this.file = new File(
                this.patternStorage.key + "/"
                        + this.patternProfile.profile.getName() + ".yml"
        );

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

    void set(String key, Object value) {
        SpartanBukkit.dataThread.execute(() -> {
            this.count++;
            this.configuration.set(null, null);

            if (this.store == 0) {
                this.store = 1;

                synchronized (patternStorage.profilesToStore) {
                    patternStorage.profilesToStore.add(this.patternProfile);
                }
            }
        });
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
