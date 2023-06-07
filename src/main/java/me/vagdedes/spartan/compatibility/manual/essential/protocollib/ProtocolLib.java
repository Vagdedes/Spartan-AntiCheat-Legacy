package me.vagdedes.spartan.compatibility.manual.essential.protocollib;

import com.comphenix.protocol.injector.server.TemporaryPlayer;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.utils.server.ReflectionUtils;
import org.bukkit.entity.Player;

public class ProtocolLib {

    private static boolean classExists = false;

    public static boolean isTemporaryPLayer(Player p) {
        return Compatibility.CompatibilityType.ProtocolLib.isFunctional()
                && canHaveTemporaryPlayers() && (p instanceof TemporaryPlayer);
    }

    public static void reload() {
        classExists = ReflectionUtils.classExists("com.comphenix.protocol.injector.server.TemporaryPlayer");

        try {
            BackgroundProtocolLib.initiate();
        } catch (Exception ignored) {
        }
    }

    public static boolean canHaveTemporaryPlayers() {
        return classExists;
    }
}
