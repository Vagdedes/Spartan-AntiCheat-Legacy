package ai.idealistic.spartan.functionality.tracking;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.functionality.moderation.AwarenessNotifications;
import ai.idealistic.spartan.functionality.server.Permissions;
import ai.idealistic.spartan.functionality.server.PluginBase;

public class LimitedEdition {

    private static final int playerLimit = 10;
    public static final String message = "";

    public static boolean canRun() {
        return !Register.isFreeEdition()
                || PluginBase.getPlayerCount() <= playerLimit;
    }

    public static void notify(PlayerProtocol protocol) {
        if (!canRun()
                && Permissions.isStaff(protocol.bukkit())) {
            AwarenessNotifications.forcefullySend(
                    protocol,
                    message,
                    true
            );
        }
    }

}
