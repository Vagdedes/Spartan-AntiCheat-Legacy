package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternProfile { // todo fix

    // todo synchronize removal of memory and storage

    private static final class Comparison {

        private final float trustFactor;
        private final long contribution;

        Comparison(float trustFactor, long contribution) {
            this.trustFactor = trustFactor;
            this.contribution = contribution;
        }
    }

    private final PatternGeneralization patternGeneralization;
    final Map<Long, Comparison> comparisons;
    final Map<Long, LinkedList<Float>> correlations;
    final PatternRepetition patternRepetition;
    private boolean loaded;
    private final File file;
    private final YamlConfiguration configuration;
    long lastContribution;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.patternGeneralization = patternGeneralization;
        this.lastContribution = 0L;

        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.comparisons = Collections.synchronizedMap(new LinkedHashMap<>());

        this.patternRepetition = new PatternRepetition();

        this.file = new File(
                Register.plugin.getDataFolder()
                        + "/comparison/"
                        + this.patternGeneralization.parent.key + "/"
                        + this.patternGeneralization.initiator.generalization + "/"
                        + this.patternGeneralization.initiator.line + "/"
                        + profile.getName() + ".yml"
        );
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        if (this.file.exists()) {
            if (this.file.isFile()) {
                this.loaded = false;

                SpartanBukkit.dataThread.execute(() -> {
                    Set<String> keys = this.configuration.getKeys(false);

                    if (!keys.isEmpty()) {
                        Set<Long> set = new LinkedHashSet<>(this.patternGeneralization.initiator.square);
                        boolean changed = false;

                        for (String key : keys) {
                            try {
                                boolean removed = false;

                                for (PatternComparison patternComparison : this.patternGeneralization.patternComparisons) {
                                    String path = key + "." + patternComparison.key;
                                    Object value = this.configuration.get(path, -1.0f);

                                    if (value instanceof Number) {
                                        long time = Long.parseLong(key);

                                        if (set.size() == this.patternGeneralization.initiator.square) {
                                            this.configuration.set(path, null);
                                            removed = true;
                                            changed = true;
                                        } else {
                                            set.add(time);
                                            // todo add
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
                            synchronized (this.keys) { // todo fix
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

    Comparison lastComparison() {
        if (!this.comparisons.isEmpty()) {
            for (Comparison comparison : this.comparisons.values()) {
                return comparison;
            }
        }
        return null;
    }

    // Save

    void updateComparison(long time, float client, float probability, long contribution, boolean storage) {
        if (this.loaded) {
            if (this.comparisons.size() == this.patternGeneralization.initiator.square) {
                Iterator<Map.Entry<Long, Comparison>> iterator = this.comparisons.entrySet().iterator();
                Map.Entry<Long, Comparison> entry = iterator.next();
                iterator.remove();

                if (storage) {
                    SpartanBukkit.dataThread.execute(() -> {
                        for (PatternComparison patternComparison : this.patternGeneralization.patternComparisons) {
                            this.configuration.set(entry.getKey() + "." + patternComparison.key, null);
                        }
                        this.configuration.set(Long.toString(entry.getKey()), null);
                    });
                }
            }
            synchronized (this.patternGeneralization.significancePie) {
                this.comparisons.put(time, new Comparison(
                        probability * this.patternGeneralization.significancePie.getProbability(client),
                        contribution
                ));
            }

            if (storage) {
                SpartanBukkit.dataThread.execute(() -> {
                    this.configuration.set(time + "." + comparison.key, value);

                    synchronized (this.patternGeneralization.profilesToStore) {
                        this.patternGeneralization.profilesToStore.add(this);
                    }
                });
            }
        }
    }

    public void updateComparison(long time, float client, float probability, long contribution) {
        this.updateComparison(time, client, probability, contribution, true);
    }

    // Separator

    public boolean contributedRecently() {
        return System.currentTimeMillis() - this.lastContribution <= TPS.tickTime;
    }

    // Separator

    void store() {
        SpartanBukkit.dataThread.execute(() -> {
            try {
                this.configuration.save(this.file);
            } catch (Exception ignored) {
            }
        });
    }

    void delete(PlayerProfile profile) {
        if (this.loaded) {
            synchronized (this.patternGeneralization.profilesToStore) {
                this.patternGeneralization.profilesToStore.remove(this);
            }
            this.file.delete();

            synchronized (this.comparisons) {
                this.comparisons.clear();
            }
            File[] files = this.patternGeneralization.parent.getLearningFiles();

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
                                            && profileName.equals(profile.getName())) {
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
