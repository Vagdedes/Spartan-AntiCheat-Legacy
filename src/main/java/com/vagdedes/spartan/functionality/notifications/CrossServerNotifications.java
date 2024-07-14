package com.vagdedes.spartan.functionality.notifications;

import com.vagdedes.spartan.abstraction.configuration.implementation.Settings;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;

public class CrossServerNotifications {

    static {
        SpartanBukkit.runRepeatingTask(() -> SpartanBukkit.connectionThread.executeIfFree(() -> {
            for (SpartanPlayer player : DetectionNotifications.getPlayers()) {
                // todo with SQL
            }
        }), 1L, 1L);
    }

    public static String getOptionValue() {
        return Config.settings.getString(Settings.crossServerNotificationsName);
    }

}
