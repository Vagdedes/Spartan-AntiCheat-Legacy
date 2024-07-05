package com.vagdedes.spartan.functionality.performance;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.check.LiveViolation;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.pattern.Pattern;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.MiningHistory;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.utils.java.TimeUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ResearchEngine {

    public static final double requiredProfiles = 10;
    private static long schedulerTicks = 0L;
    public static final Enums.DataType[] usableDataTypes = new Enums.DataType[]{
            Enums.DataType.JAVA,
            Enums.DataType.BEDROCK
    };
    private static final Map<String, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();

    static {
        SpartanBukkit.runRepeatingTask(() -> {
            if (schedulerTicks == 0) {
                schedulerTicks = 1200L;

                if (Config.sql.isEnabled()) {
                    refresh(Register.isPluginEnabled());
                } else {
                    SpartanBukkit.analysisThread.executeIfFree(() -> {
                        updateCache();
                        MainMenu.refresh();
                    });
                }
            } else {
                schedulerTicks -= 1;

                SpartanBukkit.analysisThread.executeIfFree(() -> {
                    updateCache();
                    MainMenu.refresh();
                });
            }
        }, 1L, 1L);
    }

    public static Enums.DataType[] getDynamicUsableDataTypes(boolean universal) {
        Enums.DataType dataType = SpartanEdition.getMissingDetection();
        return dataType == null ? (universal ? Enums.DataType.values() : usableDataTypes) :
                dataType == Enums.DataType.BEDROCK ? (universal ? new Enums.DataType[]{Enums.DataType.UNIVERSAL, Enums.DataType.JAVA} : new Enums.DataType[]{Enums.DataType.JAVA}) :
                        (universal ? new Enums.DataType[]{Enums.DataType.UNIVERSAL, Enums.DataType.BEDROCK} : new Enums.DataType[]{Enums.DataType.BEDROCK});
    }

    // Separator

    public static boolean enoughData() {
        return playerProfiles.size() >= requiredProfiles;
    }

    // Separator

    public static List<PlayerProfile> getPlayerProfiles() {
        return new ArrayList<>(playerProfiles.values());
    }

    public static PlayerProfile getPlayerProfile(String name) {
        PlayerProfile playerProfile = playerProfiles.get(name);

        if (playerProfile != null) {
            return playerProfile;
        }
        playerProfile = new PlayerProfile(name);
        playerProfiles.put(name, playerProfile);
        return playerProfile;
    }

    public static PlayerProfile getPlayerProfile(SpartanPlayer player, boolean force) {
        PlayerProfile playerProfile;

        if (!force) {
            playerProfile = playerProfiles.get(player.name);

            if (playerProfile != null) {
                playerProfile.update(player);
                return playerProfile;
            }
        }
        playerProfile = new PlayerProfile(player);
        playerProfiles.put(player.name, playerProfile);
        return playerProfile;
    }

    public static PlayerProfile getPlayerProfileAdvanced(String name) {
        PlayerProfile profile = playerProfiles.get(name);

        if (profile != null) {
            return profile;
        }
        if (!playerProfiles.isEmpty()) {
            for (PlayerProfile playerProfile : playerProfiles.values()) {
                if (playerProfile.getName().equalsIgnoreCase(name)) {
                    return playerProfile;
                }
            }
        }
        return null;
    }

    // Separator

    public static void resetData(Enums.HackType hackType) {
        SpartanBukkit.analysisThread.execute(() -> {
            String hackTypeString = hackType.toString();
            hackType.getCheck().clearIgnoredViolations();

            // Separator

            if (!playerProfiles.isEmpty()) {
                for (PlayerProfile playerProfile : playerProfiles.values()) {
                    playerProfile.getViolationHistory(hackType).clear();
                    playerProfile.evidence.remove(hackType, true, true);
                }
            }

            // Separator

            if (Config.sql.isEnabled()) {
                Config.sql.update("DELETE FROM " + Config.sql.getTable() + " WHERE functionality = '" + hackTypeString + "';");
            }

            // Separator

            Collection<File> files = getFiles();

            if (!files.isEmpty()) {
                for (File file : files) {
                    YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

                    for (String key : configuration.getKeys(false)) {
                        String value = configuration.getString(key);

                        if (value != null && value.contains(hackTypeString)) {
                            configuration.set(key, null);
                        }
                    }
                    try {
                        configuration.save(file);
                    } catch (Exception ignored) {
                    }
                }
            }

            // Separator
            MainMenu.refresh();
        });
    }

    public static void resetData(String playerName) {
        // Clear Violations
        SpartanProtocol p = SpartanBukkit.getProtocol(playerName);
        boolean foundPlayer = p != null;
        PlayerProfile profile;

        if (foundPlayer) {
            profile = p.getProfile();

            for (Enums.HackType hackType : Enums.HackType.values()) {
                p.spartanPlayer.getViolations(hackType).reset();
            }
        } else {
            profile = getPlayerProfile(playerName);
        }
        Pattern.deleteFromFile(profile);

        if (isStorageMode()) {
            // Clear Files/Database
            SpartanBukkit.analysisThread.execute(() -> {
                playerProfiles.remove(playerName);

                if (foundPlayer) {
                    p.setProfile(getPlayerProfile(p.spartanPlayer, true));
                }
                if (Config.sql.isEnabled()) {
                    Config.sql.update("DELETE FROM " + Config.sql.getTable() + " WHERE information LIKE '%" + playerName + "%';");
                }
                Collection<File> files = getFiles();

                if (!files.isEmpty()) {
                    for (File file : files) {
                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

                        for (String key : configuration.getKeys(false)) {
                            String value = configuration.getString(key);

                            if (value != null && value.contains(playerName)) {
                                configuration.set(key, null);
                            }
                        }
                        try {
                            configuration.save(file);
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        } else {
            playerProfiles.remove(playerName);

            if (foundPlayer) {
                p.setProfile(getPlayerProfile(p.spartanPlayer, true));
            }
        }
        MainMenu.refresh();
        InteractiveInventory.playerInfo.refresh(playerName);
    }

    // Separator

    private static String getDetectionInformation(String s) {
        String[] split = s.split("\\), \\(");
        s = split[split.length - 1];
        return s.length() < 2 ? null : s.substring(0, s.length() - 2);
    }

    private static int[] getDetectionViolationLevel(String s) {
        int index1 = s.indexOf("(" + LiveViolation.violationLevelIdentifier + " ");

        if (index1 > -1) {
            int index2 = s.indexOf(") [");

            if (index2 > -1 && index2 > index1) {
                String number = s.substring(index1 + 5, index2);
                String[] split = number.split("\\+", 3);

                if (split.length == 2) {
                    if (AlgebraUtils.validInteger(split[0])
                            && AlgebraUtils.validInteger(split[1])) {
                        return new int[]{
                                Integer.parseInt(split[0]),
                                Integer.parseInt(split[1])
                        };
                    }
                } else if (AlgebraUtils.validInteger(number)) {
                    return new int[]{
                            Integer.parseInt(number),
                            1
                    };
                }
            }
        }
        return null;
    }

    private static boolean isStorageMode() {
        return Config.settings.getBoolean("Logs.log_file") || Config.sql.isEnabled();
    }

    private static Collection<File> getFiles() {
        File[] files = new File(AntiCheatLogs.folderPath).listFiles();

        if (files != null && files.length > 0) {
            TreeMap<Integer, File> map = new TreeMap<>();
            String start = "log", end = ".yml";
            int startLength = start.length(), endLength = end.length();

            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName();

                    if (name.startsWith(start) && name.endsWith(end)) {
                        Integer integer = AlgebraUtils.returnValidInteger(name.substring(startLength, name.length() - endLength));

                        if (integer != null) {
                            map.put(integer, file);
                        }
                    }
                }
            }
            List<File> list = new LinkedList<>(map.values());
            Collections.reverse(list);
            return list;
        }
        return new ArrayList<>(0);
    }

    private static Map<String, String> getLogs() {
        Map<String, String> cache = new LinkedHashMap<>();
        int byteSize = 0;
        boolean isFull = false,
                continueWithYAML = false;

        // Separator

        if (Config.sql.isEnabled()) {
            if (!isFull) {
                try {
                    ResultSet rs = Config.sql.query("SELECT creation_date, information FROM " + Config.sql.getTable() + " ORDER BY id DESC LIMIT " + SpartanBukkit.maxSQLRows + ";");

                    if (rs != null) {
                        while (rs.next()) {
                            String data = rs.getString("information");
                            String[] all = TimeUtils.getAll(rs.getTimestamp("creation_date"));
                            String date = "(" + new Random().nextInt() + ")[" + all[0] + " " + all[1] + "]";
                            cache.put(date, data);
                            byteSize += date.length() + data.length();

                            if (byteSize >= SpartanBukkit.maxBytes) {
                                isFull = true;
                                break;
                            }
                        }
                        rs.close();

                        if (cache.isEmpty()) {
                            continueWithYAML = true;
                        }
                    } else {
                        continueWithYAML = true;
                    }
                } catch (Exception ex) {
                    continueWithYAML = true;
                }
            }
        } else {
            continueWithYAML = true;
        }

        // YAML Process

        if (!isFull && continueWithYAML) {
            Collection<File> files = getFiles();

            if (!files.isEmpty()) {
                //Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

                for (File file : files) {
                    YamlConfiguration c = YamlConfiguration.loadConfiguration(file);

                    for (String key : c.getKeys(false)) {
                        String data = c.getString(key);

                        if (data != null) {
                            cache.put(key, data);
                            byteSize += key.length() + data.length();

                            if (byteSize >= SpartanBukkit.maxBytes) {
                                isFull = true;
                                break;
                            }
                        }
                    }
                    if (isFull) {
                        break;
                    }
                }
            }
        }
        return cache;
    }

    // Separator

    public static void refresh(boolean enabledPlugin) {
        SpartanBukkit.analysisThread.executeIfFree(() -> {
            // Complete Storage
            Config.sql.refreshDatabase();

            if (enabledPlugin) {
                CloudBase.refresh(true);
                buildCache();
                Pattern.reload();
                CloudBase.refresh(false);
            } else {
                CloudBase.clear(true);
                Pattern.clear();
                ViolationAnalysis.clear();
            }
        });
    }

    private static void buildCache() {
        if (isStorageMode()) {
            Map<String, String> logs = getLogs();

            // Separator
            int size = logs.size();

            if (size > 0) {
                try {
                    String construct = Config.getConstruct();

                    for (Map.Entry<String, String> entry : logs.entrySet()) {
                        String key = entry.getKey();
                        int dateStart = key.indexOf("[") + 1;

                        if (dateStart > 0) {
                            String fullDate = key.substring(dateStart, key.length() - 1);
                            String partialDate = key.substring(dateStart, dateStart + 10);

                            String data = entry.getValue().replace(construct, "");
                            int dataLength = data.length();
                            int index = data.indexOf("]");

                            if (index > -1 && index != (dataLength - 1)) {
                                data = data.substring(index + 2);
                            }
                            int greatestSplitPosition = 10; // Attention
                            String[] split = data.split(" ", greatestSplitPosition + 1);

                            if (data.contains(" failed ")) {
                                if (split.length >= 3) {
                                    String name = split[0];
                                    String hackTypeString = split[2];
                                    PlayerProfile playerProfile = getPlayerProfile(name);

                                    for (Enums.HackType hackType : Enums.HackType.values()) {
                                        if (hackTypeString.equals(hackType.toString())) {
                                            // Separator
                                            String detection = getDetectionInformation(data);

                                            if (detection != null) {
                                                int[] violation = getDetectionViolationLevel(data);

                                                if (violation != null) {
                                                    SimpleDateFormat sdf = new SimpleDateFormat(
                                                            fullDate.contains(AntiCheatLogs.dateFormatChanger) ?
                                                                    AntiCheatLogs.dateFormat :
                                                                    AntiCheatLogs.usualDateFormat
                                                    );
                                                    playerProfile.getViolationHistory(hackType).store(
                                                            new PlayerViolation(
                                                                    sdf.parse(fullDate).getTime(),
                                                                    hackType,
                                                                    detection,
                                                                    violation[0],
                                                                    violation[1]
                                                            )
                                                    );
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            } else if (data.contains(" found ")) {
                                if (split.length >= 9) {
                                    String name = split[0].toLowerCase();

                                    try {
                                        MiningHistory.MiningOre ore = MiningHistory.getMiningOre(
                                                Material.getMaterial(
                                                        split[3].toUpperCase().replace("-", "_")
                                                )
                                        );

                                        if (ore != null) {
                                            MiningHistory miningHistory = getPlayerProfile(name).getMiningHistory(ore);

                                            if (miningHistory != null) {
                                                World.Environment environment = World.Environment.valueOf(
                                                        split[8].toUpperCase().replace("-", "_")
                                                );
                                                miningHistory.increaseMines(environment, 1, partialDate);
                                            }
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }
                    updateCache();
                    MainMenu.refresh();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void updateCache() {
        if (!playerProfiles.isEmpty()) {
            Collection<PlayerProfile> playerProfiles = new ArrayList<>(ResearchEngine.playerProfiles.values());
            Enums.DataType[] dataTypes = getDynamicUsableDataTypes(false);
            ViolationAnalysis.calculateData(playerProfiles);

            // Separator

            if (enoughData()) {
                for (Enums.HackType hackType : Enums.HackType.values()) {
                    for (Enums.DataType dataType : dataTypes) {
                        if (hackType.getCheck().isEnabled(dataType, null, null)) {
                            boolean bedrock = dataType == Enums.DataType.BEDROCK;
                            Map<Integer, ViolationAnalysis.TimePeriod> averages = new LinkedHashMap<>();

                            for (PlayerProfile playerProfile : playerProfiles) {
                                if (bedrock == playerProfile.isBedrockPlayer()
                                        && playerProfile.isLegitimate()) {
                                    Collection<PlayerViolation> list = playerProfile.getViolationHistory(hackType).getCollection();

                                    // Organize the violations into time period pieces
                                    if (!list.isEmpty()) {
                                        for (PlayerViolation playerViolation : list) {
                                            averages.computeIfAbsent(
                                                    AlgebraUtils.integerFloor(playerViolation.time / (double) hackType.violationTimeWorth),
                                                    k -> new ViolationAnalysis.TimePeriod()
                                            ).add(playerProfile, playerViolation);
                                        }
                                    }
                                }
                            }

                            if (!averages.isEmpty()) {
                                Map<Integer, Double>
                                        sum = new LinkedHashMap<>(),
                                        count = new LinkedHashMap<>();

                                // Add the average violations for the executed detections based on each time period
                                for (ViolationAnalysis.TimePeriod timePeriod : averages.values()) {
                                    Map<Integer, Double> averageViolations = timePeriod.getSquareViolations();

                                    if (!averageViolations.isEmpty()) {
                                        for (Map.Entry<Integer, Double> averageViolationsEntry : averageViolations.entrySet()) {
                                            sum.put(
                                                    averageViolationsEntry.getKey(),
                                                    sum.getOrDefault(averageViolationsEntry.getKey(), 0.0)
                                                            + averageViolationsEntry.getValue()
                                            );
                                            count.put(
                                                    averageViolationsEntry.getKey(),
                                                    count.getOrDefault(averageViolationsEntry.getKey(), 0.0)
                                                            + 1.0
                                            );
                                        }
                                    }
                                }

                                // Calculate the average violations for each
                                for (Map.Entry<Integer, Double> entry : sum.entrySet()) {
                                    entry.setValue(Math.sqrt(entry.getValue() / count.get(entry.getKey())));
                                }
                                hackType.getCheck().setIgnoredViolations(dataType, sum);
                            } else {
                                hackType.getCheck().clearIgnoredViolations(dataType);
                            }
                        } else {
                            hackType.getCheck().clearIgnoredViolations(dataType);
                        }
                    }
                }
            } else {
                for (Enums.HackType hackType : Enums.HackType.values()) {
                    hackType.getCheck().clearIgnoredViolations();
                }
            }
        } else { // We clear because there are no players
            ViolationAnalysis.clear();

            for (Enums.HackType hackType : Enums.HackType.values()) {
                hackType.getCheck().clearIgnoredViolations();
            }
        }
    }
}
