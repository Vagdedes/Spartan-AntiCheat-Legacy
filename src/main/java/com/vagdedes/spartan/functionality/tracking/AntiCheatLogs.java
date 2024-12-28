package com.vagdedes.spartan.functionality.tracking;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class AntiCheatLogs {

    private static final String dateOnlyFormat = "yyyyMMdd";
    public static final String
            dateFormat = "yyyy/MM/dd HH:mm:ss:SSSXXX",
            folderPath = Register.plugin.getDataFolder() + "/logs",
            playerIdentifier = "Player:";
    private static long lastSave = 0L;

    private static File todayFile = getFile();
    private static YamlConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(todayFile);

    public static String getDate(String pattern, long time) {
        return DateTimeFormatter.ofPattern(pattern).format(
                Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
        );
    }

    private static File getFile() {
        return new File(
                folderPath + "/log"
                        + ZonedDateTime.now().format(DateTimeFormatter.ofPattern(dateOnlyFormat))
                        + ".yml"
        );
    }

    private static void save(File file) {
        try {
            fileConfiguration.save(file);
        } catch (Exception ignored) {
        }
    }

    // Separator

    public static void shutdown() {
        if (Config.settings.getBoolean("Logs.log_file")
                && fileConfiguration != null) {
            save(todayFile);
        }
    }

    public static void rawLogInfo(long time, String information, boolean console, boolean store, boolean sql) {
        if (console && Config.settings.getBoolean("Logs.log_console")) {
            Bukkit.getConsoleSender().sendMessage(information);
        }
        if (Config.settings.getBoolean("Logs.log_file")) {
            if (store && fileConfiguration != null) {
                boolean saved = false;
                fileConfiguration.set(
                        AntiCheatLogs.getDate(AntiCheatLogs.dateFormat, time),
                        information
                );
                if (System.currentTimeMillis() - lastSave >= 5_000L) {
                    saved = true;
                    lastSave = System.currentTimeMillis();
                    File file = todayFile;

                    SpartanBukkit.dataThread.executeIfSyncElseHere(() -> {
                        lastSave = System.currentTimeMillis();
                        save(file);
                    });
                }
                File file = getFile();

                if (!file.getName().equals(todayFile.getName())) {
                    if (!saved) {
                        save(todayFile);
                    }
                    AntiCheatLogs.todayFile = file;
                    fileConfiguration = YamlConfiguration.loadConfiguration(file);
                }
            }
        }
        if (sql) {
            Config.sql.logInfo(null, null, information, null, null, System.currentTimeMillis());
        }
    }

    public static void logInfo(SpartanProtocol p,
                               String notification,
                               String information,
                               boolean console,
                               Material material,
                               Enums.HackType hackType,
                               long time) {
        rawLogInfo(time, information, console, true, false);
        Config.sql.logInfo(p, notification, information, material, hackType, time);
    }

}
