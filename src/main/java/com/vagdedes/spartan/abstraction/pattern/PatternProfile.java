package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public abstract class PatternProfile {

    private static final class Comparison {
        private long time;
        private double trustFactor;
        private float averagePatternTime;

        private Comparison(long time, double trustFactor) {
            this.time = time;
            this.trustFactor = trustFactor;
            this.averagePatternTime = -1.0f;
        }
    }

    private final PatternGeneralization generalization;
    private final Map<Long, Double> comparisons;
    private final Comparison lastComparison;
    final PatternRepetition repetition;
    private boolean loaded;
    private final File file;
    private final YamlConfiguration configuration;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.generalization = patternGeneralization;
        this.comparisons = Collections.synchronizedMap(new HashMap<>());
        this.repetition = new PatternRepetition();
        this.lastComparison = new Comparison(0L, -1.0f);
        this.file = new File(
                Register.plugin.getDataFolder()
                        + "/comparison/"
                        + this.generalization.parent.key + "/"
                        + this.generalization.scissors + "/"
                        + profile.getName() + ".yml"
        );
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        if (this.file.exists()) {
            if (this.file.isFile()) {
                this.loaded = false;

                SpartanBukkit.dataThread.execute(() -> {
                    Map<String, Object> values = this.configuration.getValues(false);

                    if (values.isEmpty()) {
                        file.delete();
                    } else {
                        int valid = 0;
                        Collection<String> remove = new ArrayList<>();

                        for (Map.Entry<String, Object> entry : values.entrySet()) {
                            if (entry.getValue() instanceof Double) {
                                try {
                                    if (valid == Pattern.comparisonDataLimit) {
                                        remove.add(entry.getKey());
                                    } else {
                                        this.updateComparison(
                                                Long.parseLong(entry.getKey()),
                                                (double) entry.getValue(),
                                                false
                                        );
                                        valid++;
                                    }
                                } catch (Exception ignored) {
                                }
                            } else {
                                remove.add(entry.getKey());
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

    protected final boolean isLoaded() {
        return this.loaded;
    }

    // Separator

    private void updateComparison(long time, double trustFactor, boolean storage) {
        if (this.comparisons.size() == Pattern.comparisonDataLimit) {
            long oldTime;

            synchronized (this.comparisons) {
                Iterator<Long> iterator = this.comparisons.keySet().iterator();
                oldTime = iterator.next();
                iterator.remove();
                this.comparisons.put(time, trustFactor);
                this.lastComparison.time = time;
                this.lastComparison.trustFactor = trustFactor;
                this.lastComparison.averagePatternTime = -1.0f;
            }

            if (storage) {
                SpartanBukkit.dataThread.execute(() -> {
                    this.configuration.set(Long.toString(oldTime), null);
                    this.configuration.set(Long.toString(time), trustFactor);
                    this.generalization.queueStorage(this);
                });
            }
        } else {
            synchronized (this.comparisons) {
                this.comparisons.put(time, trustFactor);
                this.lastComparison.time = time;
                this.lastComparison.trustFactor = trustFactor;
                this.lastComparison.averagePatternTime = -1.0f;
            }

            if (storage) {
                SpartanBukkit.dataThread.execute(() -> {
                    this.configuration.set(Long.toString(time), trustFactor);
                    this.generalization.queueStorage(this);
                });
            }
        }
    }

    protected final void updateComparison(long time, double significance, double probability, double total) {
        double existingContribution = 1.0 - (1.0 / (total + 1.0)),
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
            this.generalization.dequeueStorage(this);

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
                                                    if (this.generalization.scissors != 0) {
                                                        pattern = AlgebraUtils.cut(pattern, this.generalization.scissors);
                                                    }
                                                    this.generalization.deleteCustomLessons((int) situation, (float) pattern);
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

    float averagePatternTime() {
        if (this.lastComparison.averagePatternTime != -1.0) {
            return this.lastComparison.averagePatternTime;
        } else {
            int size = this.comparisons.size();

            if (size > 1) {
                Iterator<Long> iterator = this.comparisons.keySet().iterator();
                long previousTime = iterator.next(), currentTime;
                float total = 0.0f;

                while (iterator.hasNext()) {
                    currentTime = iterator.next();
                    total += currentTime - previousTime;
                    previousTime = currentTime;
                }
                return this.lastComparison.averagePatternTime = total / ((float) size - 1.0f);
            } else {
                return -1.0f;
            }
        }
    }

    public double trustFactor() {
        float averagePattern = this.generalization.averagePatternTime();

        if (averagePattern == -1.0f) {
            return -1.0;
        } else {
            int patternsRequired = AlgebraUtils.integerRound(
                    (System.currentTimeMillis() - this.lastComparison.time) / averagePattern
            );

            if (patternsRequired <= 1) {
                return this.lastComparison.trustFactor;
            } else {
                patternsRequired = Math.min(patternsRequired, Pattern.comparisonDataLimit);
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
