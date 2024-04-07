package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public abstract class PatternProfile {

    private static final class Comparison {
        private long time;
        private double trustFactor, averagePatternTime;

        private Comparison(long time, double trustFactor) {
            this.time = time;
            this.trustFactor = trustFactor;
            this.averagePatternTime = -1.0;
        }
    }

    final PatternGeneralization generalization;
    private final Map<Long, Double> comparisons;
    private final Comparison lastComparison;
    final PatternRepetition patternRepetition;
    private boolean loaded;
    private final File file;
    private final YamlConfiguration configuration;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.generalization = patternGeneralization;
        this.comparisons = Collections.synchronizedMap(new LinkedHashMap<>());
        this.patternRepetition = new PatternRepetition();
        this.lastComparison = new Comparison(0L, -1.0);
        this.file = new File(
                Register.plugin.getDataFolder()
                        + "/comparison/"
                        + this.generalization.parent.key + "/"
                        + this.generalization.initiator.scissors + "/"
                        + this.generalization.initiator.line + "/"
                        + profile.getName() + ".yml"
        );
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        if (this.file.exists()) {
            if (this.file.isFile()) {
                this.loaded = false;

                SpartanBukkit.dataThread.execute(() -> {
                    Set<String> keys = this.configuration.getKeys(false);

                    if (keys.isEmpty()) {
                        file.delete();
                    } else {
                        int valid = 0;
                        Collection<String> remove = new ArrayList<>();

                        for (String key : keys) {
                            try {
                                long time = Long.parseLong(key);
                                double value = this.configuration.getDouble(key, Double.MIN_VALUE);

                                if (value != Double.MIN_VALUE) {
                                    if (valid == this.generalization.initiator.square) {
                                        remove.add(key);
                                    } else {
                                        this.updateComparison(time, value, false);
                                        valid++;
                                    }
                                } else {
                                    remove.add(key);
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        if (valid == 0) {
                            this.file.delete();
                        } else if (!remove.isEmpty()) {
                            try {
                                for (String key : remove) {
                                    this.configuration.set(key, null);
                                }
                                this.configuration.save(this.file);
                            } catch (Exception ignored) {
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

    private void updateComparison(long time, double trustFactor, boolean storage) {
        if (this.comparisons.size() == this.generalization.initiator.square) {
            long oldTime;

            synchronized (this.comparisons) {
                Iterator<Map.Entry<Long, Double>> iterator = this.comparisons.entrySet().iterator();
                Map.Entry<Long, Double> entry = iterator.next();
                oldTime = entry.getKey();
                iterator.remove();
                this.comparisons.put(time, trustFactor);
                this.lastComparison.time = time;
                this.lastComparison.trustFactor = trustFactor;
                this.lastComparison.averagePatternTime = -1.0;
            }

            if (storage) {
                SpartanBukkit.dataThread.execute(() -> {
                    this.configuration.set(Long.toString(oldTime), null);
                    this.configuration.set(Long.toString(time), trustFactor);

                    synchronized (this.generalization.profilesToStore) {
                        this.generalization.profilesToStore.add(this);
                    }
                });
            }
        } else {
            synchronized (this.comparisons) {
                this.comparisons.put(time, trustFactor);
                this.lastComparison.time = time;
                this.lastComparison.trustFactor = trustFactor;
                this.lastComparison.averagePatternTime = -1.0;
            }

            if (storage) {
                SpartanBukkit.dataThread.execute(() -> {
                    this.configuration.set(Long.toString(time), trustFactor);

                    synchronized (this.generalization.profilesToStore) {
                        this.generalization.profilesToStore.add(this);
                    }
                });
            }
        }
    }

    public void updateComparison(long time, double client, ProbabilityPie pie) {
        if (this.loaded) {
            double significance;

            synchronized (this.generalization.significancePie) {
                significance = this.generalization.significancePie.getProbability(client);
            }
            double probability = pie.getProbability(client, 1.0),
                    existingContribution = 1.0 - (1.0 / (pie.getRawTotal() + 1.0)),
                    trustFactor;

            if (significance == 0.0
                    || probability == 0.0
                    || existingContribution == 0.0) {
                trustFactor = 0.0;
            } else {
                trustFactor = significance * probability * existingContribution;
            }
            this.updateComparison(time, trustFactor, true);
        }
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
            this.loaded = false;
            this.deleteCustomData();

            synchronized (this.comparisons) {
                this.comparisons.clear();
            }
            synchronized (this.generalization.profilesToStore) {
                this.generalization.profilesToStore.remove(this);
            }
            SpartanBukkit.dataThread.execute(() -> {
                this.file.delete();
                File[] files = this.generalization.parent.getLearningFiles();

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
                                        String profileName = configuration.getString(key + Pattern.profileOption, null);

                                        if (profileName != null
                                                && profileName.equals(profile.getName())) {
                                            double pattern = configuration.getDouble(key + Pattern.patternOption, Double.MIN_VALUE);

                                            if (pattern != Double.MIN_VALUE) {
                                                long situation = configuration.getLong(key + Pattern.situationOption, Long.MIN_VALUE);

                                                if (situation != Long.MIN_VALUE) {
                                                    if (this.generalization instanceof LinearPatternGeneralization) {
                                                        synchronized (((LinearPatternGeneralization) this.generalization).correlations) {
                                                            Collection<Double> list = ((LinearPatternGeneralization) this.generalization).correlations.get(situation);

                                                            if (list != null
                                                                    && list.remove(pattern)
                                                                    && list.isEmpty()) {
                                                                ((LinearPatternGeneralization) this.generalization).correlations.remove(situation);
                                                            }
                                                        }
                                                    } else {
                                                        synchronized (((NonLinearPatternGeneralization) this.generalization).correlations) {
                                                            ProbabilityPie pie = ((NonLinearPatternGeneralization) this.generalization).correlations.get(situation);

                                                            if (pie != null
                                                                    && pie.remove(pattern)
                                                                    && pie.isEmpty()) {
                                                                ((NonLinearPatternGeneralization) this.generalization).correlations.remove(situation);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            for (String option : Pattern.options) {
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
                this.loaded = true;
            });
        }
    }

    // Separator

    double averagePatternTime() {
        if (this.lastComparison.averagePatternTime != -1.0) {
            return this.lastComparison.averagePatternTime;
        } else {
            int size = this.comparisons.size();

            if (size > 1) {
                Iterator<Long> iterator = this.comparisons.keySet().iterator();
                long previousTime = iterator.next(), currentTime;
                double total = 0.0;

                while (iterator.hasNext()) {
                    currentTime = iterator.next();
                    total += currentTime - previousTime;
                    previousTime = currentTime;
                }
                return this.lastComparison.averagePatternTime = total / (size - 1.0);
            } else {
                return -1.0;
            }
        }
    }

    double trustFactor() { // todo use
        double averagePattern = this.generalization.averagePatternTime();

        if (averagePattern == -1.0) {
            return -1.0;
        } else {
            int patternsRequired = AlgebraUtils.integerRound(
                    (System.currentTimeMillis() - this.lastComparison.time) / averagePattern
            );

            if (patternsRequired <= 1) {
                return this.lastComparison.trustFactor;
            } else {
                patternsRequired = Math.min(patternsRequired, this.generalization.initiator.square);
                int dismiss = this.comparisons.size() - patternsRequired;

                if (dismiss < 0) {
                    return -1.0;
                } else {
                    double sum = 0.0;

                    synchronized (this.comparisons) {
                        Iterator<Double> iterator = this.comparisons.values().iterator();

                        for (int i = 0; i < dismiss; i++) {
                            iterator.next();
                        }

                        while (iterator.hasNext()) {
                            sum += iterator.next();
                        }
                    }
                    return sum / (double) patternsRequired;
                }
            }
        }
    }

    // Separator

    abstract void deleteCustomData();

}
