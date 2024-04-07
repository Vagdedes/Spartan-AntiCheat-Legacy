package com.vagdedes.spartan.abstraction.pattern;

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

    private static final double GB = 1024 * 1024 * 1024;
    static final String
            profileOption = ".profile",
            patternOption = ".pattern",
            situationOption = ".situation";
    static final String[] options = {profileOption, patternOption, situationOption};
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
                patternStorage.loadFromFiles();
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
    private final String learningFolder;
    private final Map<Short, PatternGeneralization> generalizations;
    private boolean load;
    private short count;
    private File file;
    private YamlConfiguration configuration;

    public Pattern(String key,
                   PatternGeneralizationInitiator initiator,
                   PatternGeneralizationInitiator[] generalizations,
                   boolean linear) {
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
                linear ? new LinearPatternGeneralization(this, initiator)
                        : new NonLinearPatternGeneralization(this, initiator)
        );

        if (generalizations.length > 0) {
            for (PatternGeneralizationInitiator loopInitiator : generalizations) {
                this.generalizations.put(
                        loopInitiator.scissors,
                        linear ? new LinearPatternGeneralization(this, loopInitiator)
                                : new NonLinearPatternGeneralization(this, loopInitiator)
                );
            }
        }
        this.reloadLearningFile();

        synchronized (instances) {
            instances.add(this);
        }
    }

    // Separator

    private void reloadLearningFile() {
        File[] files = this.getLearningFiles();

        if (files != null) {
            long length = 0;

            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                if (file.isFile() && file.getName().endsWith(".yml")) {
                    length += file.length();
                } else {
                    files[i] = null;
                }
            }

            if (length >= GB) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));

                for (File file : files) {
                    if (file != null) {
                        long fileLength = file.length();

                        if (file.delete()) {
                            length -= fileLength;

                            if (length < GB) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        int hash = Objects.hash(this.key, System.currentTimeMillis());
        this.file = new File(this.learningFolder + "/" + hash + ".yml");

        if (this.file.exists()) {
            this.reloadLearningFile();
        } else {
            this.configuration = YamlConfiguration.loadConfiguration(this.file);
        }
    }

    void set(PlayerProfile profile, long time, long situation, double pattern) {
        SpartanBukkit.dataThread.execute(() -> {
            this.count++;
            this.configuration.set(time + profileOption, profile.getName());
            this.configuration.set(time + situationOption, situation);
            this.configuration.set(time + patternOption, pattern);
        });
    }

    void storeFiles() {
        SpartanBukkit.dataThread.execute(() -> {
            try {
                this.configuration.save(this.file);

                if (this.count < 0) { // Short has overflowed (32767)
                    this.count = 0;
                    this.reloadLearningFile();
                }
            } catch (Exception ignored) {
            }
        });
        for (PatternGeneralization generalization : this.generalizations.values()) {
            generalization.storeProfiles(false);
        }
    }

    private void loadFromFiles() {
        if (this.load) {
            return;
        }
        this.load = true;
        this.storeFiles();
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
                        entry.getValue() instanceof LinearPatternGeneralization
                                ? new LinearPatternGeneralization(this, entry.getValue().initiator)
                                : new NonLinearPatternGeneralization(this, entry.getValue().initiator)
                );
            }
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                    Set<String> keys = configuration.getKeys(false);

                    if (!keys.isEmpty()) {
                        for (String key : keys) {
                            try {
                                double pattern = configuration.getDouble(key + patternOption, Double.MIN_VALUE);

                                if (pattern != Double.MIN_VALUE) {
                                    long situation = configuration.getLong(key + situationOption, Long.MIN_VALUE);

                                    if (situation != Long.MIN_VALUE) {
                                        String profileName = configuration.getString(key + profileOption, null);

                                        if (profileName != null
                                                && !profileName.isEmpty()) {
                                            long time = Long.parseLong(key);
                                            PlayerProfile profile = ResearchEngine.getPlayerProfile(profileName);

                                            for (PatternGeneralization generalization : generalizations.values()) {
                                                generalization.cache(
                                                        profile,
                                                        time,
                                                        situation,
                                                        pattern,
                                                        false
                                                );
                                            }
                                        }
                                    } else {
                                        for (String option : Pattern.options) {
                                            configuration.set(key + option, null);
                                        }
                                        configuration.set(key, null);
                                    }
                                } else {
                                    for (String option : Pattern.options) {
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
        boolean found = false;

        synchronized (this.generalizations) {
            for (PatternGeneralization generalization : this.generalizations.values()) {
                if (generalization.initiator.isEnabled()) {
                    found |= generalization.hasSituation(situation);
                }
                generalization.cache(
                        player.getProfile(),
                        time,
                        situation,
                        number.doubleValue(),
                        include
                );
            }
        }

        if (!found) {
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
