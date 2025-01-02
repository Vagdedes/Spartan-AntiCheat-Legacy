package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class JarVerification {

    private static boolean valid = true;

    public static void run(Plugin plugin) {
        if (!IDs.enabled) {
            SpartanBukkit.connectionThread.execute(() -> {
                int userID = CloudConnections.getUserIdentification();

                if (userID <= 0) {
                    valid = false;
                }
            });
        }

        if (IDs.enabled || CloudBase.hasToken()) {
            if (isValid(plugin)) {
                SpartanEdition.refresh();
            } else {
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        } else {
            SpartanEdition.refresh();

            if (!isValid(plugin)) {
                String message = "This version of " + plugin.getName() + " does not have a license."
                        + " If this download is pirated, please consider purchasing the plugin"
                        + " when your server starts making enough money. We also sell on BuiltByBit"
                        + " which supports many payment methods for all countries including yours.";
                List<SpartanProtocol> staff = Permissions.getStaff();

                if (!staff.isEmpty()) {
                    for (SpartanProtocol protocol : staff) {
                        protocol.spartan.sendImportantMessage(AwarenessNotifications.getNotification(message));
                    }
                }
                AwarenessNotifications.forcefullySend(message);
            }
        }
    }

    private static boolean isValid(Plugin plugin) {
        boolean b = valid
                && plugin.getDescription().getAuthors().toString().startsWith("[Evangelos Dedes @Vagdedes");

        try {
            String[] results = RequestUtils.get(StringUtils.decodeBase64(CloudBase.website)
                    + "?" + CloudBase.identification + "&action=add&data=userVerification");

            if (results.length > 0) {
                String line = results[0];

                if (line.equalsIgnoreCase(String.valueOf(false))) {
                    valid = false;
                    return false;
                }
                if (CloudBase.hasToken() && AlgebraUtils.validInteger(line)) {
                    IDs.setPlatform(Integer.parseInt(line));
                }
            }
        } catch (Exception e) {
            if (IDs.canAdvertise()) {
                e.printStackTrace();
            }
        }
        return b;
    }

}