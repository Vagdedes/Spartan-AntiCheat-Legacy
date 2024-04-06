package com.vagdedes.spartan.abstraction.pattern.implementation.base;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Pattern {

    private static final Collection<Pattern> instances = Collections.synchronizedList(new ArrayList<>());

    public static void delete(PlayerProfile profile) {
        synchronized (instances) {
            for (Pattern instance : instances) {
                synchronized (instance.generalizations) {
                    for (PatternGeneralization patternGeneralization : instance.generalizations.values()) {
                        PatternProfile patternProfile;

                        synchronized (patternGeneralization.profiles) {
                            patternProfile = patternGeneralization.profiles.remove(profile);
                        }
                        if (patternProfile != null) {
                            patternProfile.delete(profile);
                        }
                    }
                }
            }
        }
    }

    public static void reload() {
        synchronized (instances) {
            for (Pattern patternStorage : instances) {
                patternStorage.load();
            }
        }
    }

    public static void clear() {
        synchronized (instances) {
            for (Pattern pattern : instances) {
                synchronized (pattern.generalizations) {
                    for (PatternGeneralization generalization : pattern.generalizations.values()) {
                        generalization.clear();
                    }
                    pattern.generalizations.clear();
                }
            }
            instances.clear();
        }
    }

    // Separator

    public final String key;
    final String learningFolder;
    final Map<Short, PatternGeneralization> generalizations;
    private boolean load;

    public Pattern(String key, PatternGeneralizationInitiator initiator, PatternGeneralizationInitiator[] generalizations) {
        this.key = key;
        this.learningFolder = Register.plugin.getDataFolder()
                + "/learning/"
                + key;
        this.load = false;

        this.generalizations = Collections.synchronizedMap(
                new LinkedHashMap<>(generalizations.length + 1)
        );
        this.generalizations.put(
                (short) 0,
                new PatternGeneralizationStorage(this, initiator)
        );

        if (generalizations.length > 0) {
            for (PatternGeneralizationInitiator loopInitiator : generalizations) {
                this.generalizations.put(
                        loopInitiator.scissors,
                        new PatternGeneralizationMemory(this, loopInitiator)
                );
            }
        }

        synchronized (instances) {
            instances.add(this);
        }
    }

    // Separator

    public void store() {
        synchronized (this.generalizations) {
            ((PatternGeneralizationStorage) this.generalizations.get((short) 0)).store();
        }
        for (PatternGeneralization generalization : this.generalizations.values()) {
            generalization.storeProfiles(false);
        }
    }

    private void load() {
        if (this.load) {
            return;
        }
        this.load = true;
        store();
        Map<Short, PatternGeneralization> generalizations = new LinkedHashMap<>();
        File[] files = this.getLearningFiles();

        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            Set<Map.Entry<Short, PatternGeneralization>> entries;

            synchronized (this.generalizations) {
                entries = new HashSet<>(this.generalizations.entrySet());
            }

            for (Map.Entry<Short, PatternGeneralization> entry : entries) {
                generalizations.put(
                        entry.getKey(),
                        entry.getValue() instanceof PatternGeneralizationStorage
                                ? new PatternGeneralizationStorage(this, entry.getValue().initiator)
                                : new PatternGeneralizationMemory(this, entry.getValue().initiator)
                );
            }
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                    Set<String> keys = configuration.getKeys(false);

                    if (!keys.isEmpty()) {
                        for (String key : keys) {
                            try {
                                double pattern = configuration.getDouble(key + PatternGeneralization.patternOption, Double.MIN_VALUE);

                                if (pattern != Double.MIN_VALUE) {
                                    long situation = configuration.getLong(key + PatternGeneralization.situationOption, Long.MIN_VALUE);

                                    if (situation != Long.MIN_VALUE) {
                                        String profileName = configuration.getString(key + PatternGeneralization.profileOption, null);

                                        if (profileName != null
                                                && !profileName.isEmpty()) {
                                            long time = Long.parseLong(key);
                                            PlayerProfile profile = ResearchEngine.getPlayerProfile(profileName);

                                            for (PatternGeneralization generalization : generalizations.values()) {
                                                generalization.rawCache(
                                                        profile,
                                                        time,
                                                        situation,
                                                        pattern,
                                                        false
                                                );
                                            }
                                        }
                                    } else {
                                        for (String option : PatternGeneralization.options) {
                                            configuration.set(key + option, null);
                                        }
                                        configuration.set(key, null);
                                    }
                                } else {
                                    for (String option : PatternGeneralization.options) {
                                        configuration.set(key + option, null);
                                    }
                                    configuration.set(key, null);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        }

        if (!generalizations.isEmpty()) {
            synchronized (this.generalizations) {
                this.generalizations.clear();
                this.generalizations.putAll(generalizations);
            }
        }
        this.load = false;
    }

    // Separator

    public void learn(SpartanPlayer player, long situation, Number number) {
        long time = System.currentTimeMillis();
        boolean include;

        if (SpartanBukkit.debug) {
            include = player.isWhitelisted();
        } else {
            Integer divisor = DetectionNotifications.getDivisor(player, false);
            include = divisor == null || divisor != 69;
        }
        boolean empty = false;

        synchronized (this.generalizations) {
            for (PatternGeneralization generalization : this.generalizations.values()) {
                if (generalization.initiator.isEnabled()) {
                    synchronized (generalization.correlations) {
                        empty |= !generalization.correlations.containsKey(situation);
                    }
                }
                generalization.rawCache(
                        player.getProfile(),
                        time,
                        situation,
                        number.doubleValue(),
                        include && generalization instanceof PatternGeneralizationStorage
                );
            }
        }

        if (empty) {
            notify(player, situation);
        }
    }

    private void notify(SpartanPlayer player, long situation) {
        String message = AwarenessNotifications.getOptionalNotification(
                "Spartan's Machine Learning algorithms have insufficient data to check you. "
                        + (player.getProfile().isLegitimate()
                        ? "Continue playing LEGITIMATELY to train the algorithm so you can try again later."
                        : "Since you are " + player.getProfile().evidence.getType() + ", either clear your data via '/spartan info'"
                        + " and play LEGITIMATELY or find a legitimate player to help train the algorithm so you can try again later.")
        );

        if (message != null) {
            String s = "pattern-storage-" + this.key;

            if (AwarenessNotifications.canSend(player.uuid, s + "-" + situation, 120)
                    && AwarenessNotifications.canSend(player.uuid, s, 60)) {
                player.sendMessage(message);
            }
        }
    }

    // Separator

    public PatternGeneralization getGeneralization(Integer generalization) {
        synchronized (this.generalizations) {
            return this.generalizations.get(generalization.shortValue());
        }
    }

    File[] getLearningFiles() {
        File directory = new File(this.learningFolder);

        if (directory.exists()) {
            if (directory.isDirectory()) {
                return directory.listFiles();
            } else {
                directory.delete();
                return null;
            }
        } else {
            return null;
        }
    }
}
