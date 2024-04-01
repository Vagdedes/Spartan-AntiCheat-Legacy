package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternProfile {

    private static final class Comparison {

        private final double trustFactor;
        private final long contribution;

        Comparison(double trustFactor, long contribution) {
            this.trustFactor = trustFactor;
            this.contribution = contribution;
        }
    }

    private final PatternGeneralization patternGeneralization;
    private double trustFactorTotal;
    final Map<Long, Comparison> comparisons;
    final Map<Long, LinkedList<Number>> correlations;
    final PatternRepetition patternRepetition;
    final PlayerProfile researchProfile;
    private boolean loaded;
    private byte store;
    private final File file;
    private final YamlConfiguration configuration;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.patternGeneralization = patternGeneralization;
        this.researchProfile = profile;

        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.comparisons = Collections.synchronizedMap(new LinkedHashMap<>());

        this.patternRepetition = new PatternRepetition();
        this.trustFactorTotal = 0.0;

        this.store = 0;
        this.file = new File(
                Register.plugin.getDataFolder()
                        + "/comparison/"
                        + this.patternGeneralization.parent.key + "/"
                        + this.patternGeneralization.initiator.generalization + "/"
                        + this.patternGeneralization.initiator.individualRecentWeight + "/"
                        + this.researchProfile.getName() + ".yml"
        );
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        if (this.file.exists()) {
            if (this.file.isFile()) {
                this.loaded = false;

                SpartanBukkit.dataThread.execute(() -> {
                    Set<String> keys = this.configuration.getKeys(false);

                    if (!keys.isEmpty()) {
                        Set<Long> set = new LinkedHashSet<>(this.patternGeneralization.initiator.individualGeneralWeight);
                        boolean changed = false;

                        for (String key : keys) {
                            try {
                                boolean removed = false;

                                for (PatternComparison patternComparison : this.patternGeneralization.patternComparisons) {
                                    String path = key + "." + patternComparison.key;
                                    double value = this.configuration.getDouble(path, -1.0);

                                    if (value != -1.0) {
                                        long time = Long.parseLong(key);

                                        if (set.size() == this.patternGeneralization.initiator.individualGeneralWeight) {
                                            this.configuration.set(path, null);
                                            removed = true;
                                            changed = true;
                                        } else {
                                            set.add(time);
                                            patternComparison.put(
                                                    this,
                                                    value,
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

    double contribution(boolean compare) {
        int required = this.patternGeneralization.initiator.individualRecentWeight + 1;

        if (this.comparisons.size() < required) {
            return -1.0;
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
                    ? total / (double) size
                    : -1.0;
        }
    }

    double trustFactor(boolean compare) {
        if (this.comparisons.size() < this.patternGeneralization.initiator.individualRecentWeight) {
            return -1.0;
        } else if (compare) {
            return this.trustFactorTotal / (double) this.comparisons.size();
        } else {
            double total = 0.0;
            Iterator<Comparison> iterator = this.comparisons.values().iterator();

            if (this.comparisons.size() > this.patternGeneralization.initiator.individualRecentWeight) {
                for (int i = 0; i < this.comparisons.size() - this.patternGeneralization.initiator.individualRecentWeight; i++) {
                    iterator.next();
                }
            }
            while (iterator.hasNext()) {
                total += iterator.next().trustFactor;
            }
            return total / (double) this.patternGeneralization.initiator.individualRecentWeight;
        }
    }

    void refreshComparisons(long time, double trustFactor, long contribution) {
        this.trustFactorTotal += trustFactor;

        if (this.comparisons.size() == this.patternGeneralization.initiator.individualGeneralWeight) {
            Iterator<Long> iterator = this.comparisons.keySet().iterator();
            this.trustFactorTotal -= iterator.next();
            iterator.remove();
        }
        this.comparisons.put(time, new Comparison(trustFactor, contribution));
    }

    // Separator

    void set(PatternComparison comparison, long time, Object value) {
        if (this.loaded) {
            SpartanBukkit.dataThread.execute(() -> {
                this.configuration.set(time + "." + comparison.key, value);

                if (this.store == 0) {
                    this.store = 1;

                    synchronized (this.patternGeneralization.parent.profilesToStore) {
                        this.patternGeneralization.parent.profilesToStore.add(this);
                    }
                }
            });
        }
    }

    void store() {
        if (this.store == 1) {
            this.store = 2;

            SpartanBukkit.dataThread.execute(() -> {
                try {
                    Collection<String> keys = new ArrayList<>();

                    synchronized (this.comparisons) {
                        int delete = this.comparisons.size() - this.patternGeneralization.initiator.individualGeneralWeight;

                        if (delete > 0) {
                            Iterator<Long> iterator = this.comparisons.keySet().iterator();

                            while (iterator.hasNext() && delete-- > 0) {
                                keys.add(Long.toString(iterator.next()));
                                iterator.remove();
                            }
                        }
                    }
                    if (!keys.isEmpty()) {
                        for (String key : keys) {
                            for (PatternComparison patternComparison : this.patternGeneralization.patternComparisons) {
                                this.configuration.set(key + "." + patternComparison.key, null);
                            }
                            this.configuration.set(key, null);
                        }
                    }
                    this.configuration.save(this.file);
                    this.store = 0;
                } catch (Exception ignored) {
                }
            });
        }
    }

    void delete() {
        if (loaded) {
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
                                            && profileName.equals(this.researchProfile.getName())) {
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
