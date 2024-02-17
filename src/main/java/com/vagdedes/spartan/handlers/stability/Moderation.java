package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickablemessage.ClickableMessage;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;

import java.util.List;

public class Moderation { // todo simplify and transfer to player object

    public static final String
            punishmentMessage = " was punished with the commands: ",
            warningMessage = " was warned for ",
            kickMessage = " was kicked for ",
            falsePositiveDisclaimer = "§4(False Positive)§f ",
            criticalHitMessage = " landed a critical hit with ";

    private static boolean isDetected(SpartanPlayer p, boolean checkSilent, long ms) {
        LiveViolation liveViolation = p.getLastViolation();
        return liveViolation.getLastViolationTime(true) <= ms
                && (!checkSilent
                || !liveViolation.hackType.getCheck().isSilent(p.getWorld().getName(), p));
    }

    public static boolean wasDetected(SpartanPlayer p) {
        return isDetected(p, false, 105L);
    }

    public static boolean isDetectedAndPrevented(SpartanPlayer p) {
        return isDetected(p, true, 55L);
    }

    public static void detection(SpartanPlayer detectedPlayer,
                                 Check check,
                                 String detailedDetection,
                                 String info,
                                 int level,
                                 boolean log,
                                 boolean falsePositive,
                                 boolean canPrevent,
                                 boolean hacker) {
        if (level < Check.maxViolationsPerCycle) {
            // Variables
            String name = detectedPlayer.getName(),
                    world = detectedPlayer.getWorld().getName();
            boolean individualOnlyNotifications = Config.settings.getBoolean("Notifications.individual_only_notifications");
            int cancelViolation = CancelViolation.get(check.getHackType(), detectedPlayer.getDataType()),
                    playerCount = SpartanBukkit.getPlayerCount();

            // Message Preparation
            String shorterMessage, message = Config.messages.getColorfulString("detection_notification");

            if (ConfigUtils.contains(message, "{info}")) {
                if (falsePositive) {
                    message = falsePositiveDisclaimer + ConfigUtils.replaceWithSyntax(detectedPlayer, message, check.getHackType());
                } else {
                    message = ConfigUtils.replaceWithSyntax(detectedPlayer, message, check.getHackType());
                }

                // Separator
                message = message.replace("{info}", info);

                if (individualOnlyNotifications) {
                    shorterMessage = message;
                } else {
                    shorterMessage = detailedDetection != null ? message.replace("{info}", detailedDetection) : message;
                }
            } else {
                if (falsePositive) {
                    message = falsePositiveDisclaimer + ConfigUtils.replaceWithSyntax(detectedPlayer, message, check.getHackType());
                } else {
                    message = ConfigUtils.replaceWithSyntax(detectedPlayer, message, check.getHackType());
                }
                shorterMessage = message;
            }

            // Cross-Server & Console Notifications
            boolean cannotPunish = !check.supportsLiveEvidence(),
                    consoleLog = !falsePositive && level >= cancelViolation,
                    important = hacker
                            || cannotPunish
                            || canPrevent
                            || consoleLog
                            || detectedPlayer.getProfile().isSuspected();

            if (important) {
                CrossServerInformation.queueNotification(message, true);
            }

            // Logs
            if (log) {
                SpartanLocation location = detectedPlayer.getLocation();
                String cancelViolationString = (canPrevent ? "-" : "") + cancelViolation + (check.hasCancelViolation() ? "*" : ""),
                        information = (falsePositive ? "(False Positive) " : "")
                                + Config.getConstruct() + name + " failed " + check.getHackType() + " (VL: " + level + ") " +
                                "[(Version: " + MultiVersion.fork() + " " + MultiVersion.versionString() + "), (C-V: " + cancelViolationString + ") (Silent: " + check.isSilent(world, detectedPlayer) + "), " +
                                "(Ping: " + detectedPlayer.getPing() + "ms), (TPS: " + AlgebraUtils.cut(TPS.get(detectedPlayer, false), 3) + "), (Hacker: " + hacker + "), (Online: " + playerCount + "), " +
                                "(XYZ: " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), (" + info + ")]";
                AntiCheatLogs.logInfo(detectedPlayer, information, consoleLog ? shorterMessage : null, null, check.getHackType(), falsePositive, true, level, cancelViolation);
            }

            // Local Notifications
            String command = Config.settings.getString("Notifications.message_clickable_command").replace("{player}", name);

            if (individualOnlyNotifications) {
                Integer divisor = DetectionNotifications.getDivisor(detectedPlayer, false);

                if (DetectionNotifications.canAcceptMessages(detectedPlayer, divisor, false)
                        && isDivisorValid(level, divisor, hacker || falsePositive || cannotPunish || TestServer.isIdentified())) { // Attention
                    ClickableMessage.sendCommand(detectedPlayer, message, "Command: " + command, command);
                }
            } else {
                List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers(false);

                if (!notificationPlayers.isEmpty()) {
                    boolean testServer = TestServer.isIdentified(),
                            uncertain = hacker || falsePositive || testServer || cannotPunish; // Attention

                    for (SpartanPlayer staff : notificationPlayers) {
                        int divisor = NotifyViolation.get(staff, detectedPlayer, cancelViolation, playerCount, testServer);

                        if (isDivisorValid(level, divisor, uncertain)) {
                            ClickableMessage.sendCommand(
                                    staff,
                                    divisor == 1 ? message : shorterMessage,
                                    "Command: " + command,
                                    command
                            );
                        }
                    }
                }
            }
        }
    }

    private static boolean isDivisorValid(int level, int divisor, boolean uncertain) {
        if (level > 0) {
            return divisor > 0 && (level % divisor) == 0 // Basic
                    || divisor < 0 && level >= -divisor; // Secret
        }
        return uncertain && (divisor == 0 || divisor == 1); // Uncertain
    }
}
