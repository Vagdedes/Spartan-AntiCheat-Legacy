package me.vagdedes.spartan.features.synchronicity;

import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.notifications.AwarenessNotifications;
import me.vagdedes.spartan.features.synchronicity.cloud.CloudConnections;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.IDs;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.java.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SpartanEdition {

    private static final boolean
            java = true, // false for bedrock
            fullEdition = false; // change this when necessary

    private static long checkTime = 0L;
    public static final String[] jarNames = new String[]{
            StringUtils.decodeBase64("U3BhcnRhbkJlZHJvY2tFZGl0aW9u"), // Bedrock
            StringUtils.decodeBase64("U3BhcnRhbkZ1bGxFZGl0aW9u"), // Full
            "Spartan", // Last because it is contained in all previous names
    };
    private static final String productLink = StringUtils.decodeBase64("aHR0cHM6Ly92YWdkZWRlcy5jb20vYWNjb3VudC92aWV3UHJvZHVjdC8/aWQ9");
    private static final String
            type = "{type}",
            productID = "{product-id}",
            notificationKey = "alternative-version",
            notificationMessage = "\n§cHey, just a heads up! You have " + type + " players which cannot be checked by the anti-cheat."
                    + (SpartanBukkit.canAdvertise ? "\nClick §n" + productLink + productID + " §r§cto fix this." : "")
                    + "\nSet '" + Settings.showEcosystemOption.replace(".", " -> ") + "' to 'false' via '" + Settings.getFile().getName() + "' to disable this notification.";
    private static final boolean javaEdition;
    public static final boolean freeAlternativeVersion;
    private static boolean notify = false, alternativeVersion;

    static {
        if (SpartanBukkit.canAdvertise) {
            javaEdition = java || fullEdition;
            alternativeVersion = fullEdition;
            freeAlternativeVersion = false;
        } else { // SpigotMC
            javaEdition = true;
            alternativeVersion = false;
            freeAlternativeVersion = true;
        }
    }

    // Verification

    public static void refresh() {
        if (SpartanBukkit.canAdvertise && !alternativeVersion) {
            SpartanBukkit.connectionThread.execute(() -> {
                if (CloudConnections.ownsProduct(
                        getProductID(getOppositeVersion())
                )) {
                    alternativeVersion = true;
                }
                for (Enums.HackType hackType : Enums.HackType.values()) {
                    hackType.resetCheck(false);
                }
            });
        }
    }

    // Detections

    public static boolean supportsCheck(ResearchEngine.DataType dataType, Enums.HackType hackType) {
        switch (dataType) {
            case Java:
                return true;
            case Bedrock:
                switch (hackType) {
                    case EntityMove:
                    case IrregularMovements:
                    case NoFall:
                    case Speed:
                    case NoSlowdown:
                        return true;
                    case XRay:
                    case KillAura:
                    case Exploits:
                    case NoSwing:
                    case FastEat:
                    case FastClicks:
                    case Velocity:
                    case Criticals:
                    case FastBreak:
                    case BlockReach:
                    case ImpossibleActions:
                    case FastPlace:
                    case FastBow:
                    case FastHeal:
                    case ItemDrops:
                    case AutoRespawn:
                    case ImpossibleInventory:
                    case MorePackets:
                        return !freeAlternativeVersion;
                    default: // HitReach, GhostHand, InventoryClicks
                        return false;
                }
            default:
                return false;
        }
    }

    public static boolean hasDetectionsPurchased(ResearchEngine.DataType dataType) {
        switch (dataType) {
            case Java:
                return javaEdition || alternativeVersion;
            case Bedrock:
                return !javaEdition || alternativeVersion;
            default:
                return false;
        }
    }

    public static ResearchEngine.DataType getMissingDetection() {
        return hasDetectionsPurchased(ResearchEngine.DataType.Java) ?
                (hasDetectionsPurchased(ResearchEngine.DataType.Bedrock) ? null : ResearchEngine.DataType.Bedrock) :
                ResearchEngine.DataType.Java;
    }

    public static ResearchEngine.DataType getVersion() {
        return javaEdition ? ResearchEngine.DataType.Java : ResearchEngine.DataType.Bedrock;
    }

    public static ResearchEngine.DataType getOppositeVersion() {
        return javaEdition ? ResearchEngine.DataType.Bedrock : ResearchEngine.DataType.Java;
    }

    // Product

    public static String getProductID(ResearchEngine.DataType dataType) {
        switch (dataType) {
            case Bedrock:
                return "16";
            case Java:
                return "1";
            default:
                return null;
        }
    }

    public static String getProductName(boolean cloudFeature) {
        ResearchEngine.DataType missingDetection = getMissingDetection();

        if (missingDetection != null) {
            switch (missingDetection) {
                case Java:
                    return cloudFeature ?
                            getProductID(ResearchEngine.DataType.Bedrock) :
                            "Spartan: " + ResearchEngine.DataType.Bedrock + " Edition";
                case Bedrock:
                    return cloudFeature ?
                            getProductID(ResearchEngine.DataType.Java) :
                            "Spartan: " + ResearchEngine.DataType.Java + " Edition";
                default:
                    break;
            }
        }
        return cloudFeature ?
                getProductID(ResearchEngine.DataType.Java) :
                "Spartan: " + ResearchEngine.DataType.Java + " & " + ResearchEngine.DataType.Bedrock + " Edition";
    }

    // Notifications

    public static void attemptNotification(SpartanPlayer player) {
        if (!IDs.isPreview()) {
            ResearchEngine.DataType missingDetection = getMissingDetection();

            if (missingDetection != null && Settings.getBoolean(Settings.showEcosystemOption)) {
                if (notify) {
                    sendNotification(player, missingDetection);
                } else {
                    long time = System.currentTimeMillis();

                    if ((time - checkTime) >= 60_000L) {
                        checkTime = time;
                        boolean bedrockIsMissingDetection = missingDetection == ResearchEngine.DataType.Bedrock;

                        if (bedrockIsMissingDetection == player.isBedrockPlayer()) {
                            notify = true;
                            sendNotification(player, missingDetection);
                        } else {
                            List<SpartanPlayer> players = SpartanBukkit.getPlayers();
                            players.remove(player);
                            int size = players.size();
                            List<PlayerProfile> checkedProfiles = new ArrayList<>(size);

                            if (size > 0) {
                                for (SpartanPlayer otherPlayer : players) {
                                    if (bedrockIsMissingDetection == otherPlayer.isBedrockPlayer()) {
                                        notify = true;
                                        sendNotification(player, missingDetection);
                                        return;
                                    } else {
                                        checkedProfiles.add(player.getProfile());
                                    }
                                }
                            }

                            // Separator

                            List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

                            if (playerProfiles.size() > 0) {
                                playerProfiles.remove(player.getProfile());
                                playerProfiles.removeAll(checkedProfiles);

                                if (playerProfiles.size() > 0) {
                                    for (PlayerProfile profile : playerProfiles) {
                                        if (bedrockIsMissingDetection == profile.isBedrockPlayer()) {
                                            notify = true;
                                            sendNotification(player, missingDetection);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void sendNotification(SpartanPlayer player, ResearchEngine.DataType dataType) {
        if (Permissions.isStaff(player)) {
            String message = AwarenessNotifications.getNotification(
                    notificationMessage.replace(type, dataType.toString()).replace(productID, getProductID(dataType))
            );

            if (AwarenessNotifications.canSend(player.getUniqueId(), notificationKey)) {
                player.sendImportantMessage(message);
            }
        }
    }
}
