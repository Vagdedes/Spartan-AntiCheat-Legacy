package com.vagdedes.spartan.functionality.synchronicity;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.configuration.Settings;
import com.vagdedes.spartan.functionality.important.Permissions;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudConnections;
import com.vagdedes.spartan.handlers.connection.IDs;
import com.vagdedes.spartan.handlers.stability.ResearchEngine;
import com.vagdedes.spartan.objects.profiling.PlayerProfile;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SpartanEdition {

    private static final boolean
            java = true, // false for bedrock
            fullEdition = false; // change this when necessary

    private static long checkTime = 0L;
    public static final String[] jarNames = new String[]{
            StringUtils.decodeBase64("U3BhcnRhbjJKYXZhRWRpdGlvbg=="), // Java 2.0
            StringUtils.decodeBase64("U3BhcnRhbkJlZHJvY2tFZGl0aW9u"), // Bedrock 1.0
            StringUtils.decodeBase64("U3BhcnRhbjJCZWRyb2NrRWRpdGlvbg=="), // Bedrock 2.0
            StringUtils.decodeBase64("U3BhcnRhbkZ1bGxFZGl0aW9u"), // Full
            "Spartan", // (Java 1.0) Last because it is contained in all previous names
    };
    private static final String productLink = StringUtils.decodeBase64("aHR0cHM6Ly92YWdkZWRlcy5jb20vYWNjb3VudC92aWV3UHJvZHVjdC8/aWQ9");
    private static final String
            type = "{type}",
            productID = "{product-id}",
            notificationKey = "alternative-version",
            notificationMessage = "\n§cHey, just a heads up! You have " + type + " players which cannot be checked by the anti-cheat."
                    + (SpartanBukkit.canAdvertise ? "\nClick §n" + productLink + productID + " §r§cto fix this." : "")
                    + "\nSet '" + Settings.showEcosystemOption.replace(".", " -> ") + "' to 'false' via '" + Config.settings.getFile().getName() + "' to disable this notification.";
    private static final boolean javaEdition;
    private static boolean
            notify = false,
            v_2_0 = false,
            alternativeVersion;

    static {
        if (SpartanBukkit.canAdvertise) {
            javaEdition = java || fullEdition;
            alternativeVersion = fullEdition;
        } else { // SpigotMC
            javaEdition = true;
            alternativeVersion = false;
        }
    }

    // Verification

    public static void refresh() {
        Runnable runnable = () -> {
            if (SpartanBukkit.canAdvertise && !alternativeVersion) {
                ResearchEngine.DataType dataType = getOppositeVersion();

                if (CloudConnections.ownsProduct(
                        get2_0_ProductID(dataType)
                )) {
                    alternativeVersion = true;
                    v_2_0 = true;
                    Config.messages.clear();
                } else if (CloudConnections.ownsProduct(
                        get1_0_ProductID(dataType)
                )) {
                    alternativeVersion = true;
                    Config.messages.clear();
                } else if (CloudConnections.ownsProduct(
                        get2_0_ProductID(getVersion())
                )) {
                    v_2_0 = true;
                }
                for (Enums.HackType hackType : Enums.HackType.values()) {
                    hackType.resetCheck();
                }
            }
        };

        if (SpartanBukkit.isSynchronised()) {
            SpartanBukkit.connectionThread.execute(runnable);
        } else {
            runnable.run();
        }
    }

    // 2.0

    public static boolean is2ndGeneration() {
        return v_2_0;
    }

    public static String getGeneration() {
        return is2ndGeneration() ? "2.0" : "1.0";
    }

    // Detections

    public static boolean supportsCheck(ResearchEngine.DataType dataType, Enums.HackType hackType) {
        switch (dataType) {
            case Java:
                return true;
            case Bedrock:
                switch (hackType) {
                    case IrregularMovements:
                    case NoFall:
                    case Speed:
                    case NoSlowdown:
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
                        return true;
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
        return is2ndGeneration() ? get2_0_ProductID(dataType) : get1_0_ProductID(dataType);
    }


    public static String get1_0_ProductID(ResearchEngine.DataType dataType) {
        switch (dataType) {
            case Bedrock:
                return "16";
            default:
                return "1";
        }
    }

    public static String get2_0_ProductID(ResearchEngine.DataType dataType) {
        switch (dataType) {
            case Bedrock:
                return "22";
            default:
                return "21";
        }
    }

    public static String getProductName(boolean cloudFeature) {
        ResearchEngine.DataType missingDetection = getMissingDetection();

        if (missingDetection != null) {
            switch (missingDetection) {
                case Java:
                    return cloudFeature ?
                            getProductID(ResearchEngine.DataType.Bedrock) :
                            "Spartan " + getGeneration() + ": " + ResearchEngine.DataType.Bedrock + " Edition";
                case Bedrock:
                    return cloudFeature ?
                            getProductID(ResearchEngine.DataType.Java) :
                            "Spartan " + getGeneration() + ": " + ResearchEngine.DataType.Java + " Edition";
                default:
                    break;
            }
        }
        return cloudFeature ?
                getProductID(ResearchEngine.DataType.Java) :
                "Spartan " + getGeneration() + ": " + ResearchEngine.DataType.Java + " & " + ResearchEngine.DataType.Bedrock + " Edition";
    }

    // Notifications

    public static void attemptNotification(SpartanPlayer player) {
        if (!IDs.isPreview()) {
            ResearchEngine.DataType missingDetection = getMissingDetection();

            if (missingDetection != null && Config.settings.getBoolean(Settings.showEcosystemOption)) {
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
                            List<PlayerProfile> checkedProfiles;

                            if (size > 0) {
                                checkedProfiles = new ArrayList<>(size);

                                for (SpartanPlayer otherPlayer : players) {
                                    if (bedrockIsMissingDetection == otherPlayer.isBedrockPlayer()) {
                                        notify = true;
                                        sendNotification(player, missingDetection);
                                        return;
                                    } else {
                                        checkedProfiles.add(player.getProfile());
                                    }
                                }
                            } else {
                                checkedProfiles = new ArrayList<>(0);
                            }

                            // Separator

                            List<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

                            if (!playerProfiles.isEmpty()) {
                                playerProfiles.remove(player.getProfile());
                                playerProfiles.removeAll(checkedProfiles);

                                if (!playerProfiles.isEmpty()) {
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
                    notificationMessage.replace(type, dataType.toString()).replace(productID, get1_0_ProductID(dataType)) // Keep 1.0 here as it is the base product
            );

            if (AwarenessNotifications.canSend(player.getUniqueId(), notificationKey)) {
                player.sendImportantMessage(message);
            }
        }
    }

}
