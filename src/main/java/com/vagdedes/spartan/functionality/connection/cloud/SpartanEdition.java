package com.vagdedes.spartan.functionality.connection.cloud;

import com.vagdedes.spartan.abstraction.configuration.implementation.Settings;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.ArrayList;
import java.util.List;

public class SpartanEdition {

    private static final Enums.DataType dataType = Enums.DataType.Java,
            oppositeType = dataType == Enums.DataType.Java ? Enums.DataType.Bedrock : Enums.DataType.Java;

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
    private static boolean
            notify = false,
            alternativeVersion = false;
    private static final boolean[] secondGeneration = new boolean[2];

    // Verification

    public static void refresh() {
        Runnable runnable = () -> {
            if (SpartanBukkit.canAdvertise && !alternativeVersion) {
                boolean changed = false;

                if (CloudConnections.ownsProduct(
                        get2_0_ProductID(dataType)
                )) {
                    secondGeneration[dataType.ordinal()] = true;
                    changed = true;
                }

                if (CloudConnections.ownsProduct(
                        get2_0_ProductID(oppositeType)
                )) {
                    alternativeVersion = true;
                    secondGeneration[oppositeType.ordinal()] = true;
                    changed = true;
                } else if (CloudConnections.ownsProduct(
                        get1_0_ProductID(oppositeType)
                )) {
                    alternativeVersion = true;
                    changed = true;
                }

                if (changed) {
                    Config.messages.clear();

                    for (Enums.HackType hackType : Enums.HackType.values()) {
                        hackType.resetCheck();
                    }
                }
            }
        };

        if (SpartanBukkit.isSynchronised()) {
            SpartanBukkit.connectionThread.execute(runnable);
        } else {
            runnable.run();
        }
    }

    // General

    public static boolean is2ndGeneration() {
        return secondGeneration[dataType.ordinal()]
                || secondGeneration[oppositeType.ordinal()];
    }

    // Detections

    public static boolean supportsCheck(Enums.DataType dataType, Enums.HackType hackType) {
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

    public static boolean hasDetectionsPurchased(Enums.DataType dataType) {
        return SpartanEdition.dataType == dataType || alternativeVersion;
    }

    public static Enums.DataType getMissingDetection() {
        return alternativeVersion ? null : oppositeType;
    }

    // Product

    private static String get1_0_ProductID(Enums.DataType dataType) {
        switch (dataType) {
            case Bedrock:
                return "16";
            default:
                return "1";
        }
    }

    private static String get2_0_ProductID(Enums.DataType dataType) {
        switch (dataType) {
            case Bedrock:
                return "22";
            default:
                return "21";
        }
    }

    public static String getProductName(boolean cloudFeature) {
        if (alternativeVersion) {
            if (secondGeneration[dataType.ordinal()]) {
                if (secondGeneration[oppositeType.ordinal()]) {
                    return cloudFeature ? get2_0_ProductID(dataType) : "Spartan AntiCheat 2.0: Java & Bedrock Edition";
                } else {
                    return cloudFeature ? get2_0_ProductID(dataType) : "Spartan AntiCheat 2.0: " + dataType + " Edition | Spartan AntiCheat 1.0: " + oppositeType + " Edition";
                }
            } else if (secondGeneration[oppositeType.ordinal()]) {
                return cloudFeature ? get2_0_ProductID(oppositeType) : "Spartan AntiCheat 1.0: " + dataType + " Edition | Spartan AntiCheat 2.0: " + oppositeType + " Edition";
            } else {
                return cloudFeature ? get1_0_ProductID(dataType) : "Spartan AntiCheat 1.0: Java & Bedrock Edition";
            }
        } else if (secondGeneration[dataType.ordinal()]) {
            return cloudFeature ? get2_0_ProductID(dataType) : "Spartan AntiCheat 2.0: " + dataType + " Edition";
        } else {
            return cloudFeature ? get1_0_ProductID(dataType) : "Spartan AntiCheat 1.0: " + dataType + " Edition";
        }
    }

    // Notifications

    public static void attemptNotification(SpartanPlayer player) {
        Enums.DataType missingDetection = getMissingDetection();

        if (missingDetection != null
                && Config.settings.getBoolean(Settings.showEcosystemOption)) {
            if (notify) {
                sendNotification(player, missingDetection);
            } else {
                long time = System.currentTimeMillis();

                if ((time - checkTime) >= 60_000L) {
                    checkTime = time;
                    boolean bedrockIsMissingDetection = missingDetection == Enums.DataType.Bedrock;

                    if (bedrockIsMissingDetection == player.bedrockPlayer) {
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
                                if (bedrockIsMissingDetection == otherPlayer.bedrockPlayer) {
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

    private static void sendNotification(SpartanPlayer player, Enums.DataType dataType) {
        if (Permissions.isStaff(player)) {
            String message = AwarenessNotifications.getNotification(
                    notificationMessage.replace(type, dataType.toString()).replace(productID, get1_0_ProductID(dataType)) // Keep 1.0 here as it is the base product
            );

            if (AwarenessNotifications.canSend(player.uuid, notificationKey)) {
                player.sendImportantMessage(message);
            }
        }
    }

}
