package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternProfile {

    private static final class Comparison {
        private long time;
        private float trustFactor;

        private Comparison(long time, float trustFactor) {
            this.time = time;
            this.trustFactor = trustFactor;
        }
    }

    private final PatternGeneralization generalization;
    private final PlayerProfile researchProfile;
    private final Map<Long, Float> comparisons;
    private final Comparison lastComparison;
    private float trustFactorTotal;
    final Map<Long, LinkedList<Float>> correlations;
    final PatternRepetition patternRepetition;
    private boolean loaded;
    private final File file;
    private final YamlConfiguration configuration;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.generalization = patternGeneralization;
        this.researchProfile = profile;
        this.trustFactorTotal = 0.0f;

        this.correlations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.comparisons = Collections.synchronizedMap(new LinkedHashMap<>());

        this.patternRepetition = new PatternRepetition();
        this.lastComparison = new Comparison(0L, -1.0f);

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

                    if (!keys.isEmpty()) {
                        int count = 0;
                        boolean changed = false;

                        for (String key : keys) {
                            try {
                                long time = Long.parseLong(key);
                                Object value = this.configuration.get(key, -1.0f);

                                if (value instanceof Number) {
                                    if (count == this.generalization.initiator.square) {
                                        this.configuration.set(key, null);
                                        changed = true;
                                    } else {
                                        this.updateComparison(time, ((Number) value).floatValue(), false);
                                        count++;
                                    }
                                } else {
                                    this.configuration.set(key, null);
                                    changed = true;
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        if (changed) {
                            try {
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

    private void updateComparison(long time, float trustFactor, boolean storage) {
        this.lastComparison.time = time;
        this.lastComparison.trustFactor = trustFactor;

        if (this.comparisons.size() == this.generalization.initiator.square) {
            long oldTime;

            synchronized (this.comparisons) {
                Iterator<Map.Entry<Long, Float>> iterator = this.comparisons.entrySet().iterator();
                Map.Entry<Long, Float> entry = iterator.next();
                oldTime = entry.getKey();
                iterator.remove();
                this.comparisons.put(time, trustFactor);
                this.trustFactorTotal -= entry.getValue();
                this.trustFactorTotal += trustFactor;
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
                this.trustFactorTotal += trustFactor;
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

    public void updateComparison(long time, float client, ProbabilityPie pie) {
        if (this.loaded) {
            float significance;

            synchronized (this.generalization.significancePie) {
                significance = this.generalization.significancePie.getProbability(client);
            }
            float probability = pie.getProbability(client, 1.0f),
                    existingContribution = 1.0f - (1.0f / pie.getRawTotal() + 1.0f);
            this.updateComparison(
                    time,
                    significance * probability * existingContribution,
                    true
            );
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
            synchronized (this.generalization.profilesToStore) {
                this.generalization.profilesToStore.remove(this);
            }
            this.file.delete();

            synchronized (this.comparisons) {
                this.comparisons.clear();
            }
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

    // Separator

    float averageTrustFactor(long timePeriod) {
        if (this.researchProfile.isOnline()) {
            if (System.currentTimeMillis() - this.lastComparison.time <= timePeriod) {
                return this.lastComparison.trustFactor;
            } else {
                int dismiss = this.comparisons.size() - this.generalization.initiator.line;

                if (dismiss < 0) {
                    return -1.0f;
                } else {
                    float sum = 0.0f;

                    synchronized (this.comparisons) {
                        Iterator<Float> iterator = this.comparisons.values().iterator();

                        for (int i = 0; i < dismiss; i++) {
                            iterator.next();
                        }

                        while (iterator.hasNext()) {
                            sum += iterator.next();
                        }
                    }
                    return sum / (float) this.generalization.initiator.line;
                }
            }
        } else if (this.comparisons.size() >= this.generalization.initiator.square - this.generalization.initiator.line) {
            return this.trustFactorTotal / (float) this.comparisons.size();
        } else {
            return -1.0f;
        }
    }

}
