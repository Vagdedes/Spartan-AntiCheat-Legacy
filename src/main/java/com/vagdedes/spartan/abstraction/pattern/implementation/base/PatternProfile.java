package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
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
    private float trustFactorTotal;
    final Map<Long, Comparison> comparisons;
    final Map<Long, LinkedList<Float>> correlations;
    final PatternRepetition patternRepetition;
    private byte store;
    private final File file;
    private final YamlConfiguration configuration;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.patternGeneralization = patternGeneralization;

        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.comparisons = Collections.synchronizedMap(new LinkedHashMap<>());

        this.patternRepetition = new PatternRepetition();
        this.trustFactorTotal = 0.0f;

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
                this.store = -1;

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
                                            patternComparison.put(
                                                    this,
                                                    ((Number) value).floatValue(),
                                                    time,
                                                    false
                                            );
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
                            synchronized (this.keys) {
                                this.keys.clear();
                                this.keys.addAll(set);
                            }
                        }
                    }
                    this.store = 0;
                });
            } else {
                this.file.delete();
                this.store = 0;
            }
        } else {
            this.store = 0;
        }
    }

    float contribution(boolean compare) {
        int required = this.patternGeneralization.initiator.line + 1;

        if (this.comparisons.size() < required) {
            return -1.0f;
        } else {
            Iterator<Comparison> iterator = this.comparisons.values().iterator();
            int size = this.comparisons.size();
            long total = 0L, previous = -1L;

            if (compare) {
                int dismiss = size - required;

                if (dismiss > 0) {
                    for (int i = 0; i < dismiss; i++) {
                        iterator.next();
                        size--;
                    }
                }
            }
            while (iterator.hasNext()) {
                long current = iterator.next().contribution;

                if (current == -1L) {
                    size--;
                } else {
                    if (previous == -1L) {
                        size--;
                    } else {
                        total += current - previous;
                    }
                    previous = current;
                }
            }
            return size >= required
                    ? (float) (total / (double) size)
                    : -1.0f;
        }
    }

    float trustFactor(boolean compare) {
        if (this.comparisons.size() < this.patternGeneralization.initiator.line) {
            return -1.0f;
        } else if (compare) {
            return this.trustFactorTotal / (float) this.comparisons.size();
        } else {
            float total = 0.0f;
            Iterator<Comparison> iterator = this.comparisons.values().iterator();

            if (this.comparisons.size() > this.patternGeneralization.initiator.line) {
                for (int i = 0; i < this.comparisons.size() - this.patternGeneralization.initiator.line; i++) {
                    iterator.next();
                }
            }
            while (iterator.hasNext()) {
                total += iterator.next().trustFactor;
            }
            return (float) (total / (double) this.patternGeneralization.initiator.line);
        }
    }

    void refreshComparisons(long time, float trustFactor, long contribution) {
        this.trustFactorTotal += trustFactor;

        if (this.comparisons.size() == this.patternGeneralization.initiator.square) {
            Iterator<Map.Entry<Long, Comparison>> iterator = this.comparisons.entrySet().iterator();
            Map.Entry<Long, Comparison> entry = iterator.next();
            iterator.remove();
            this.trustFactorTotal -= entry.getValue().trustFactor;

            SpartanBukkit.dataThread.execute(() -> {
                for (PatternComparison patternComparison : this.patternGeneralization.patternComparisons) {
                    this.configuration.set(entry.getKey() + "." + patternComparison.key, null);
                }
                this.configuration.set(Long.toString(entry.getKey()), null);
                this.queue();
            });
        }
        this.comparisons.put(time, new Comparison(trustFactor, contribution));
    }

    // Separator

    void set(PatternComparison comparison, long time, Object value) {
        if (this.store != -1) {
            SpartanBukkit.dataThread.execute(() -> {
                this.configuration.set(time + "." + comparison.key, value);
                this.queue();
            });
        }
    }

    private void queue() {
        if (this.store == 0) {
            this.store = 1;

            synchronized (this.patternGeneralization.parent.profilesToStore) {
                this.patternGeneralization.parent.profilesToStore.add(this);
            }
        }
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

    void delete(PlayerProfile profile) {
        if (this.store != -1) {
            synchronized (this.patternGeneralization.parent.profilesToStore) {
                this.patternGeneralization.parent.profilesToStore.remove(this);
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
