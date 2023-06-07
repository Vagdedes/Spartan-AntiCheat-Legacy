package me.vagdedes.spartan.compatibility.necessary.bedrock.plugins;

import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.notifications.AwarenessNotifications;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.utils.server.ReflectionUtils;
import org.geysermc.floodgate.FloodgateAPI;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;
import java.util.UUID;

public class Floodgate {

    public static final String bedrockDetectionsDisabled = "Bedrock Detections are disabled";
    private static int version = 0;

    public static void reload() {
        if (ReflectionUtils.classExists("org.geysermc.floodgate.FloodgateAPI")) {
            version = 1;
        } else if (ReflectionUtils.classExists("org.geysermc.floodgate.api.FloodgateApi")) {
            version = 2;
        } else {
            version = 0;
            return;
        }
        boolean floodgate = PluginUtils.contains("floodgate"), geyser = PluginUtils.contains("geyser");

        if (geyser && !floodgate) {
            String message = AwarenessNotifications.getOptionalNotification(
                    "'Geyser' plugin allows Bedrock players to join your Java Minecraft server. "
                            + "In order for the Bedrock compatibility to enable, you must also install a plugin named 'Floodgate', which will allow Spartan to identify who is a bedrock player.");

            if (message != null) {
                List<SpartanPlayer> players = Permissions.getStaff();

                if (players.size() > 0) {
                    for (SpartanPlayer p : players) {
                        if (AwarenessNotifications.canSend(p.getUniqueId(), "floodgate")) {
                            p.sendMessage(message);
                        }
                    }
                }
            }
        }
    }

    public static boolean isUsingBedrockDetections(boolean checkPlayers) {
        if (Compatibility.CompatibilityType.Floodgate.isFunctional()) {
            if (checkPlayers) {
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (players.size() > 0) {
                    for (SpartanPlayer p : players) {
                        if (p.isBedrockPlayer()) {
                            return true;
                        }
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean isBedrockPlayer(UUID uuid, String name) {
        if (Compatibility.CompatibilityType.Floodgate.isFunctional()
                && (version == 1 ? FloodgateAPI.isBedrockPlayer(uuid) :
                version == 2 && FloodgateApi.getInstance().isFloodgatePlayer(uuid))) {
            return true;
        }
        return name != null && isBedrockPlayer(name) || uuid.toString().startsWith("00000000-0000-");
    }

    public static boolean isBedrockPlayer(String name) {
        String prefix = Settings.getString("Important.bedrock_player_prefix");
        return prefix != null && prefix.length() > 0 && name.startsWith(prefix);
    }
}
