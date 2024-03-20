package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PatternProfileStorage {

    private final PatternStorage patternStorage;
    private final PatternProfile patternProfile;
    private final File file;
    private YamlConfiguration configuration;
    private byte store;

    PatternProfileStorage(PatternStorage patternStorage, PatternProfile patternProfile) {
        this.patternStorage = patternStorage;
        this.patternProfile = patternProfile;
        this.file = new File(
                Register.plugin.getDataFolder()
                        + patternStorage.key + "/statistics/"
                        + patternProfile.profile.getName() + ".yml"
        );
        this.store = 0;
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
        this.reload();
    }

    private void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
    }

    void set(long time, String key, Object value) {
        SpartanBukkit.dataThread.execute(() -> {
            this.configuration.set(time + "." + key, value);

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
                } catch (Exception ignored) {
                }
            });
        }
    }

    void delete() {
        synchronized (this.patternStorage.profilesToStore) {
            this.patternStorage.profiles.remove(this.patternProfile.profile);
        }
        file.delete();
        this.reload();
    }
}
