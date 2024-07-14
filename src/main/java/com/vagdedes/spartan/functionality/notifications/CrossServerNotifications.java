package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.configuration.implementation.Settings;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.OverflowList;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CrossServerNotifications {

    private static String serverName = null;

    static {
        SpartanBukkit.runRepeatingTask(() -> SpartanBukkit.dataThread.execute(() -> {
            for (SpartanPlayer player : DetectionNotifications.getPlayers()) {
                // todo
            }
        }), 1L, 1L);
    }

    public static void refresh() {
        serverName = null;
    }

    public static boolean isOptionValid(String option) {
        return !option.isEmpty()
                && !option.equals("specify server name")
                && !option.equals("false");
    }

    public static String getOptionValue() {
        if (serverName != null) {
            return serverName;
        }
        String option = Config.settings.getString(Settings.cloudServerNameOption);
        int length = option.length();

        if (length > 0) {
            if (length > 32) {
                option = option.substring(0, 32);
            }
            return serverName = StringUtils.getClearColorString(option);
        }
        return serverName = "";
    }

}
