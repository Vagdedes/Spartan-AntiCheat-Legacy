package ai.idealistic.vacan.functionality.connection;

import ai.idealistic.vacan.Register;
import ai.idealistic.vacan.abstraction.check.Check;
import ai.idealistic.vacan.abstraction.check.CheckEnums;
import ai.idealistic.vacan.abstraction.profiling.PlayerProfile;
import ai.idealistic.vacan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.vacan.functionality.moderation.AwarenessNotifications;
import ai.idealistic.vacan.functionality.server.Config;
import ai.idealistic.vacan.functionality.server.PluginBase;
import ai.idealistic.vacan.functionality.tracking.ResearchEngine;
import ai.idealistic.vacan.utils.java.ReflectionUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PluginEdition {

    private static final String[] JavaIDs = {
            "25638",
            "11196",
            "350"
    };
    private static final Check.DataType
            currentType,
            alternativeType;

    private static long checkTime = 0L;
    public static final String patreonURL = "https://www.patreon.com/VacanAntiCheat";
    private static final int notificationCooldown = 60 * 60;
    private static final String
            type = "{type}",
            product = "{product}",
            versionNotificationMessage = "\n§cHey, just a heads up!"
                    + " You have " + type + " players which cannot be checked by the anti-cheat due to missing " + type + " detections.";
    private static boolean
            notifyCache = false,
            oneEdition = false,
            alternativeVersion = false;
    private static final CheckEnums.HackCategoryType[] forSaleChecks = {
            CheckEnums.HackCategoryType.COMBAT,
            CheckEnums.HackCategoryType.MOVEMENT,
            CheckEnums.HackCategoryType.WORLD
    };
    private static final boolean[][] checks = new boolean[Check.DataType.values().length][forSaleChecks.length];

    static {
        Check.DataType current = Check.DataType.BEDROCK;

        if (!IDs.enabled
                && Bukkit.getMotd().contains(Register.pluginName)) {
            current = Check.DataType.JAVA;
        } else {
            for (String id : JavaIDs) {
                if (id.equals(IDs.resource)) {
                    current = Check.DataType.JAVA;
                    break;
                }
            }
        }
        currentType = current;
        alternativeType = currentType == Check.DataType.JAVA
                ? Check.DataType.BEDROCK
                : Check.DataType.JAVA;
    }

    // Verification

    public static void refresh() {
        PluginBase.connectionThread.executeIfUnknownThreadElseHere(() -> {
            boolean subscriptionBased = CloudConnections.isSubscriptionBased();

            if (!oneEdition) {
                oneEdition = subscriptionBased || CloudConnections.ownsVacanOne();
            }

            for (CheckEnums.HackCategoryType category : forSaleChecks) {
                if (checks[currentType.ordinal()][category.ordinal()]
                        && checks[alternativeType.ordinal()][category.ordinal()]) {
                    continue;
                }
                if (!subscriptionBased
                        && Config.settings.exists("Purchases.opt_out_from_subscriptions")
                        && Config.settings.getBoolean("Purchases.opt_out_from_subscriptions")) {
                    checks[currentType.ordinal()][category.ordinal()] = true;
                    checks[alternativeType.ordinal()][category.ordinal()] = true;
                    continue;
                }
                String[] reply = CloudConnections.getOwnedChecks(category.toString());

                if (reply != null) {
                    for (String potentialDataType : reply) {
                        for (Check.DataType dataType : Check.DataType.values()) {
                            if (potentialDataType.equals(dataType.toString())) {
                                checks[dataType.ordinal()][category.ordinal()] = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!alternativeVersion
                    && oneEdition) {
                String pluginsFolder = Register.plugin.getDataFolder().toString().replace(
                        Register.pluginName,
                        ""
                );

                for (File file : new File(pluginsFolder).listFiles()) {
                    if (file.isFile()
                            && file.getName().endsWith(".jar")) {
                        for (String clazz : new String[]{
                                IDs.class.getCanonicalName(),
                                "ai.idealistic.filegui.system.IDs"
                        }) {
                            Object object = ReflectionUtils.getFieldFromJar(
                                    file.getAbsolutePath(),
                                    clazz,
                                    "user"
                            );

                            if (object != null
                                    && object.toString().equals(IDs.user)) {
                                object = ReflectionUtils.getFieldFromJar(
                                        file.getAbsolutePath(),
                                        clazz,
                                        "resource"
                                );

                                if (object != null
                                        && !object.toString().equals(IDs.resource)) {
                                    if (currentType == Check.DataType.JAVA) {
                                        boolean java = false;

                                        for (String id : JavaIDs) {
                                            if (id.equals(object.toString())) {
                                                java = true;
                                                break;
                                            }
                                        }
                                        if (!java) {
                                            alternativeVersion = true;
                                            break;
                                        }
                                    } else {
                                        for (String id : JavaIDs) {
                                            if (id.equals(object.toString())) {
                                                alternativeVersion = true;
                                                break;
                                            }
                                        }

                                        if (alternativeVersion) {
                                            break;
                                        }
                                    }
                                }
                            }

                            if (alternativeVersion) {
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    // Detections

    public static boolean hasDetectionsPurchased(Check.DataType dataType) {
        return PluginEdition.currentType == dataType || alternativeVersion;
    }

    public static boolean hasDetectionsPurchased(Check.DataType dataType, CheckEnums.HackCategoryType category) {
        return hasDetectionsPurchased(dataType)
                && (category.ordinal() >= forSaleChecks.length
                || checks[dataType.ordinal()][category.ordinal()]);
    }

    // Product

    public static String getProductID() {
        return currentType == Check.DataType.JAVA
                ? "1"
                : "16";
    }

    public static String getProductName(String context) {
        String lastColor = null;
        boolean color = false;

        for (char c : context.toCharArray()) {
            if (c == '§') {
                color = true;
            } else if (color) {
                lastColor = Character.toString(c);
                color = false;
            }
        }
        return Register.pluginName
                + ": §a" + currentType
                + "§8/" + (alternativeVersion ? "§a" : "§c") + alternativeType
                + (lastColor != null ? "§" + lastColor : "");
    }

    // Notifications

    public static void attemptNotifications(PlayerProtocol protocol) {
        Check.DataType[] missingDetections = !alternativeVersion
                ? new Check.DataType[]{alternativeType}
                : new Check.DataType[]{};

        if (missingDetections.length > 0) {
            if (notifyCache) {
                attemptVersionNotification(protocol, missingDetections[0]);
                return;
            }
            long time = System.currentTimeMillis();

            if ((time - checkTime) >= 60_000L) {
                checkTime = time;

                if (missingDetections[0] == protocol.getDataType()) {
                    notifyCache = true;
                    attemptVersionNotification(protocol, missingDetections[0]);
                    return;
                }
                Collection<PlayerProtocol> players = PluginBase.getProtocols();
                players.remove(protocol);
                int size = players.size();
                List<PlayerProfile> checkedProfiles;

                if (size > 0) {
                    checkedProfiles = new ArrayList<>(size);

                    for (PlayerProtocol otherProtocol : players) {
                        if (missingDetections[0] == otherProtocol.getDataType()) {
                            notifyCache = true;
                            attemptVersionNotification(otherProtocol, missingDetections[0]);
                            return;
                        } else {
                            checkedProfiles.add(otherProtocol.profile());
                        }
                    }
                } else {
                    checkedProfiles = new ArrayList<>(0);
                }

                // Separator

                Collection<PlayerProfile> playerProfiles = ResearchEngine.getPlayerProfiles();

                if (!playerProfiles.isEmpty()) {
                    playerProfiles.remove(protocol.profile());
                    playerProfiles.removeAll(checkedProfiles);

                    if (!playerProfiles.isEmpty()) {
                        for (PlayerProfile profile : playerProfiles) {
                            if (profile.getLastDataType() == missingDetections[0]) {
                                notifyCache = true;
                                attemptVersionNotification(protocol, missingDetections[0]);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void attemptVersionNotification(PlayerProtocol protocol, Check.DataType dataType) {
        String message = AwarenessNotifications.getOptionalNotification(
                (versionNotificationMessage
                        + (IDs.canAdvertise()
                        ? " Click §n" + product + "§r§c to §lfix this§r§c."
                        : " Visit §n" + DiscordServer.url + "§r§c to §lfix this§r§c."))
                        .replace(type, dataType.toString())
                        .replace(
                                product,
                                IDs.builtByBit
                                        ? (dataType == Check.DataType.JAVA
                                        ? "https://builtbybit.com/resources/12832"
                                        : "https://builtbybit.com/resources/11196")
                                        : (dataType == Check.DataType.JAVA
                                        ? "https://polymart.org/resource/3600"
                                        : "https://polymart.org/resource/350")
                        )
        );

        if (message != null
                && AwarenessNotifications.canSend(protocol.getUUID(), "alternative-version", notificationCooldown)) {
            protocol.sendImportantMessage(message);
        }
    }

}
