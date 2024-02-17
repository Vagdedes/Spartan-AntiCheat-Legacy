package com.vagdedes.spartan.handlers.stability;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickablemessage.ClickableMessage;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.objects.system.Check;
import com.vagdedes.spartan.objects.system.LiveViolation;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import com.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Moderation { // todo simplify and transfer to player object

    public static final String
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

    public static void warn(CommandSender punisher, SpartanPlayer target, String reason) {
        String punisherName = punisher instanceof ConsoleCommandSender
                ? Config.messages.getColorfulString("console_name")
                : punisher.getName(),
                warning = ConfigUtils.replaceWithSyntax(target,
                        Config.messages.getColorfulString("warning_message")
                                .replace("{reason}", reason)
                                .replace("{punisher}", punisherName),
                        null),
                feedback = ConfigUtils.replaceWithSyntax(target,
                        Config.messages.getColorfulString("warning_feedback_message")
                                .replace("{reason}", reason)
                                .replace("{punisher}", punisherName),
                        null);
        target.sendMessage(warning);
        punisher.sendMessage(feedback);

        SpartanLocation location = target.getLocation();
        CrossServerInformation.queueNotificationWithWebhook(target.getUniqueId(), target.getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                "Warning", reason,
                true);

        AntiCheatLogs.logInfo(Config.getConstruct() + target.getName() + warningMessage + reason);
        target.getProfile().getPunishmentHistory().increaseWarnings();
    }

    public static void kick(CommandSender punisher, SpartanPlayer target, String reason) {
        String punisherName = punisher instanceof ConsoleCommandSender
                ? Config.messages.getColorfulString("console_name")
                : punisher.getName(),
                kick = ConfigUtils.replaceWithSyntax(target,
                        Config.messages.getColorfulString("kick_reason")
                                .replace("{reason}", reason)
                                .replace("{punisher}", punisherName),
                        null),
                announcement = ConfigUtils.replaceWithSyntax(target,
                        Config.messages.getColorfulString("kick_broadcast_message")
                                .replace("{reason}", reason)
                                .replace("{punisher}", punisherName),
                        null);

        if (Config.settings.getBoolean("Punishments.broadcast_on_punishment")) {
            Bukkit.broadcastMessage(announcement);
        } else {
            List<SpartanPlayer> players = SpartanBukkit.getPlayers();

            if (!players.isEmpty()) {
                for (SpartanPlayer o : players) {
                    if (DetectionNotifications.hasPermission(o)) {
                        o.sendMessage(announcement);
                    }
                }
            }
        }

        SpartanLocation location = target.getLocation();
        CrossServerInformation.queueNotificationWithWebhook(target.getUniqueId(), target.getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                "Kick", reason,
                true);

        AntiCheatLogs.logInfo(Config.getConstruct() + target.getName() + kickMessage + reason);
        target.getProfile().getPunishmentHistory().increaseKicks();
        target.kickPlayer(kick);
    }

    public static void detection(SpartanPlayer detectedPlayer,
                                 Check check,
                                 String detailedDetection,
                                 String info,
                                 int level,
                                 boolean log, boolean falsePositive, boolean canPrevent, boolean hacker) {
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

                if (notificationPlayers.size() > 0) {
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

    public static void performPunishments(SpartanPlayer p, HackType hackType, int violation, ResearchEngine.DataType dataType) {
        Check check = hackType.getCheck();

        if (check.canPunish()) {
            boolean legacy = Config.isLegacy();
            Collection<String> commands = legacy ? check.getLegacyCommands(violation) : check.getCommands();

            if (!commands.isEmpty()) {
                Player n = p.getPlayer();

                if (n != null && n.isOnline()) {
                    boolean performed = false, found = false;
                    UUID uuid = p.getUniqueId();
                    String name = p.getName();
                    SpartanLocation location = p.getLocation();

                    if (legacy) {
                        boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");

                        for (String command : commands) {
                            if (command != null) {
                                found = true;
                                String modifiedCommand = ConfigUtils.replaceWithSyntax(p, command, hackType);

                                if (enabledDeveloperAPI) {
                                    PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(n, hackType, new HackType[]{hackType}, modifiedCommand);
                                    Register.manager.callEvent(event);

                                    if (event.isCancelled()) {
                                        continue;
                                    }
                                }
                                performed = true;
                                SpartanBukkit.runDelayedTask(p, () -> {
                                    if (p != null) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                                    }
                                }, 1);
                            }
                        }
                    } else {
                        Collection<HackType> detectedHacks = Config.getPunishableHackModules(p, hackType, violation, dataType);

                        if (!detectedHacks.isEmpty()) {
                            boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");
                            HackType[] hackTypes = detectedHacks.toArray(new HackType[0]);
                            StringBuilder stringBuilder = new StringBuilder();

                            for (HackType detectedHack : detectedHacks) {
                                stringBuilder.append(detectedHack.getCheck().getName()).append(", ");
                            }
                            String detections = stringBuilder.substring(0, stringBuilder.length() - 2);

                            for (String command : commands) {
                                if (command != null) {
                                    found = true;
                                    String modifiedCommand = ConfigUtils.replaceWithSyntax(
                                            p,
                                            command.replaceAll("\\{detections}|\\{detection}", detections),
                                            null
                                    );

                                    if (enabledDeveloperAPI) {
                                        PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(n, hackType, hackTypes, modifiedCommand);
                                        Register.manager.callEvent(event);

                                        if (event.isCancelled()) {
                                            continue;
                                        }
                                    }

                                    performed = true;
                                    SpartanBukkit.runDelayedTask(p, () -> {
                                        if (p != null) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                                        }
                                    }, 1);
                                }
                            }
                        }
                    }

                    if (performed) {
                        String commandsString = StringUtils.toString(commands, "\n");

                        if (!commandsString.isEmpty()) {
                            SpartanBukkit.runDelayedTask(p, () ->
                                    CrossServerInformation.queueWebhook(uuid, name,
                                            location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                                            "Punishment", commandsString), 2);
                        }
                    } else if (found && AwarenessNotifications.canSend(SpartanBukkit.uuid, hackType + "-cancelled-punishment-event")) {
                        String notification = "Just a reminder that the punishments of the '" + hackType + "' check were just cancelled via code by a third-party plugin."
                                + " Please do not reach support for this as it relates only to your server.";
                        List<Plugin> dependentPlugins = PluginUtils.getDependentPlugins(Register.plugin.getName());

                        if (!dependentPlugins.isEmpty()) {
                            StringBuilder dependentPluginNames = new StringBuilder();

                            for (Plugin plugin : dependentPlugins) {
                                dependentPluginNames.append(dependentPluginNames.length() == 0 ? "" : ", ").append(plugin.getName());
                            }
                            notification += " Here are possible plugins that could be doing this:\n" + dependentPluginNames;
                        }
                        AwarenessNotifications.forcefullySend(notification);
                    }
                }
            } else if (AwarenessNotifications.canSend(SpartanBukkit.uuid, hackType + "-no-punishment-commands")) {
                AwarenessNotifications.forcefullySend("Just a reminder that you have set no punishment commands for the '" + hackType + "' check.");
            }
        } else if (check.canPunish() && AwarenessNotifications.canSend(SpartanBukkit.uuid, hackType + "-disabled-punishments")) {
            AwarenessNotifications.forcefullySend("Just a reminder that punishments have been disabled for the '" + hackType + "' check.");
        }
    }

    // Handlers

    private static boolean isDivisorValid(int level, int divisor, boolean uncertain) {
        if (level > 0) {
            return divisor > 0 && (level % divisor) == 0 // Basic
                    || divisor < 0 && level >= -divisor; // Secret
        }
        return uncertain && (divisor == 0 || divisor == 1); // Uncertain
    }
}
