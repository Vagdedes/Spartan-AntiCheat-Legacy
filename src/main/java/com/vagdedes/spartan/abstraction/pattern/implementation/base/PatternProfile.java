package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.ProbabilityPie;
import com.vagdedes.spartan.utils.math.ProbabilityRank;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PatternProfile {

    private final PatternGeneralization patternGeneralization;
    final Map<Long, Float> comparisons;
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
                        int count = 0;
                        boolean changed = false;

                        for (String key : keys) {
                            try {
                                long time = Long.parseLong(key);
                                Object value = this.configuration.get(key, -1.0f);

                                if (value instanceof Number) {
                                    if (count == this.patternGeneralization.initiator.square) {
                                        this.configuration.set(key, null);
                                        changed = true;
                                    } else {
                                        this.updateComparison(time, ((Number) value).floatValue(), false);
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

    // Save

    private void updateComparison(long time, float trustFactor, boolean storage) {
        if (this.loaded) {
            if (this.comparisons.size() == this.patternGeneralization.initiator.square) {
                Iterator<Long> iterator = this.comparisons.keySet().iterator();
                long oldTime = iterator.next();
                iterator.remove();

                if (storage) {
                    SpartanBukkit.dataThread.execute(
                            () -> this.configuration.set(Long.toString(oldTime), null)
                    );
                }
            }
            this.comparisons.put(time, trustFactor);

            if (storage) {
                SpartanBukkit.dataThread.execute(() -> {
                    this.configuration.set(Long.toString(time), trustFactor);

                    synchronized (this.patternGeneralization.profilesToStore) {
                        this.patternGeneralization.profilesToStore.add(this);
                    }
                });
            }
        }
    }

    public void updateComparison(long time, float client, ProbabilityPie pie) {
        float significance;

        synchronized (this.patternGeneralization.significancePie) {
            significance = this.patternGeneralization.significancePie.getProbability(client);
        }
        float probability = pie.getProbability(client, 1.0f),
                existingContribution = 1.0f - (1.0f / pie.getRawTotal() + 1.0f);
        this.updateComparison(
                time,
                significance * probability * existingContribution,
                true
        );
    }

    public void updateComparison(long time, float client, ProbabilityRank rank) {
        float significance;

        synchronized (this.patternGeneralization.significancePie) {
            significance = this.patternGeneralization.significancePie.getProbability(client);
        }
        float probability = rank.getPosition(client, 1.0f),
                existingContribution = 1.0f - (1.0f / rank.getRawTotal() + 1.0f);
        this.updateComparison(
                time,
                significance * probability * existingContribution,
                true
        );
    }

    // Separator

    private boolean contributedRecently() {
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
