package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.configuration.ConfigurationSection;
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

    final int hash;
    private final PatternGeneralization generalization;
    final PatternRepetition repetition;

    PatternProfile(PatternGeneralization patternGeneralization, PlayerProfile profile) {
        this.hash = profile.getName().hashCode();
        this.generalization = patternGeneralization;
        this.repetition = new PatternRepetition();
    }

    // Separator

    protected final void finishComparison(long time,
                                          PlayerProfile profile,
                                          int situation,
                                          float pattern,
                                          double probability,
                                          int dataSize) {
        double existingContribution = (1.0 - AlgebraUtils.contribution(dataSize))
                        * (1.0 - this.generalization.uniqueContribution(situation, pattern));

        if (this.generalization.parent.key.endsWith("vertical")) {
            profile.getSpartanPlayer().debug(
                    false,
                    false,
                    1.0 - (probability * existingContribution)
            );
        }
    }

    // Separator

    final synchronized void delete(PlayerProfile profile) {
        this.deleteCustomData();

        SpartanBukkit.dataThread.executeWithPriority(() -> {
            File[] files = this.generalization.parent.getLearningFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()
                            && file.getName().endsWith(".yml")) {
                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                        Set<String> keys = configuration.getKeys(false);

                        if (!keys.isEmpty()) {
                            boolean changed = false;

                            for (String key : keys) {
                                ConfigurationSection section = configuration.getConfigurationSection(key);

                                if (section == null) {
                                    continue;
                                }
                                Map<String, Object> data = section.getValues(false);

                                if (data.size() != 3) {
                                    this.generalization.parent.deleteFromFile(configuration, key);
                                    changed = true;
                                    continue;
                                }
                                Object patternObject = data.get(Pattern.patternOption),
                                        situation = data.get(Pattern.situationOption),
                                        profileObject = data.get(Pattern.profileOption);

                                if (!(patternObject instanceof Double || patternObject instanceof Float)
                                        || !(situation instanceof Integer || situation instanceof Long || situation instanceof Short)
                                        || !(profileObject instanceof String)) {
                                    this.generalization.parent.deleteFromFile(configuration, key);
                                    changed = true;
                                    continue;
                                }
                                try {
                                    if (profileObject.toString().equals(profile.getName())) {
                                        double pattern = (double) patternObject;

                                        if (this.generalization.scissors != 0) {
                                            pattern = AlgebraUtils.cut(pattern, this.generalization.scissors);
                                        }
                                        this.generalization.deletePattern((int) situation, this, (float) pattern);
                                        this.generalization.parent.deleteFromFile(configuration, key);
                                        changed = true;
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            if (changed) {
                                try {
                                    configuration.save(file);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    // Separator

    abstract void deleteCustomData();

}
