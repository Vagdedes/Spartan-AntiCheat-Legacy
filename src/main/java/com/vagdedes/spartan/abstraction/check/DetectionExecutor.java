package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import me.vagdedes.spartan.api.API;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DetectionExecutor extends CheckDetection {

    public final String name;
    private final boolean def;
    private long notifications;
    private final Map<Check.DataType, Double> probability;
    private final Map<Check.DataType, Map<Long, PlayerViolation>> data;

    public DetectionExecutor(CheckExecutor executor, String name, boolean def) {
        super(executor);
        this.name = name;
        this.def = def;
        this.probability = new ConcurrentHashMap<>(Check.DataType.values().length);
        this.data = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
        this.isEnabled();
        this.storeDetection(executor);
    }

    public DetectionExecutor(DetectionExecutor executor, String name, boolean def) {
        super(executor.executor);
        this.name = name;
        this.def = def;
        this.probability = new ConcurrentHashMap<>(Check.DataType.values().length);
        this.data = Collections.synchronizedMap(
                new LinkedHashMap<>(Check.DataType.values().length)
        );
        this.isEnabled();
        this.storeDetection(executor.executor);
    }

    private void storeDetection(CheckExecutor executor) {
        if (this.name == null) {
            while (true) {
                if (executor.detections.putIfAbsent(Integer.toString(new Random().nextInt()), this) == null) {
                    break;
                }
            }
        } else if (executor.detections.putIfAbsent(this.name, this) != null) {
            throw new IllegalArgumentException(
                    "Detection '" + this.name + "' already exists for enum '" + executor.hackType.toString() + "'."
            );
        }
    }

    @Override
    public int hashCode() {
        return this.hackType.hashCode() * SpartanBukkit.hashCodeMultiplier
                + (this.name == null ? 0 : this.name.hashCode());
    }

    // Check

    public final boolean isEnabled() {
        return this.hackType.getCheck().isEnabled(
                this.player == null ? null : this.player.dataType,
                this.player == null ? null : this.player.getWorld().getName()
        )
                && (this.name == null
                || this.hackType.getCheck().getBooleanOption("check_" + this.name, this.def));
    }

    // Data

    public final boolean hasDataToCompare(Check.DataType dataType) {
        Map<Long, PlayerViolation> map = this.data.get(dataType);
        return map != null && map.size() > 1;
    }

    public final void store(Check.DataType dataType, PlayerViolation playerViolation) {
        synchronized (this.data) {
            Map<Long, PlayerViolation> map = this.data.computeIfAbsent(
                    dataType,
                    k -> new TreeMap<>()
            );

            if (map.size() == 1_000) {
                Iterator<Long> iterator = map.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
            map.put(playerViolation.time, playerViolation);
        }
        ResearchEngine.queueToCache(this.hackType);
    }

    public final Double getTimeDifference(Check.DataType dataType) {
        if (this.hasDataToCompare(dataType)) {
            double squareSum = 0.0;
            int size;

            synchronized (this.data) {
                Map<Long, PlayerViolation> map = this.data.get(dataType);
                size = map.size();
                Iterator<PlayerViolation> iterator = map.values().iterator();
                long previous = iterator.next().time;

                while (iterator.hasNext()) {
                    PlayerViolation violation = iterator.next();
                    double difference = Math.min(
                            (violation.time - previous) / violation.increase,
                            60_000L
                    );
                    previous = violation.time;
                    squareSum += difference * difference;
                }
            }
            return Math.sqrt(squareSum / (size - 1.0));
        } else {
            return null;
        }
    }

    // Probability

    public final boolean surpassedProbability(Check.DataType dataType, double threshold) {
        return PlayerEvidence.surpassedProbability(this.getProbability(dataType), threshold);
    }

    public final void clearProbability(Check.DataType dataType) {
        this.probability.put(dataType, PlayerEvidence.emptyProbability);
    }

    public final void setProbability(Check.DataType dataType, double probability) {
        this.probability.put(dataType, probability);
    }

    public final double getProbability(Check.DataType dataType) {
        return isEnabled()
                ? this.probability.getOrDefault(dataType, PlayerEvidence.emptyProbability)
                : PlayerEvidence.emptyProbability;
    }

    // Violation

    final void violate(HackPrevention newPrevention, String information, double increase, long time) {
        CancelCause disableCause = this.executor.getDisableCause();

        if (disableCause == null
                || disableCause.hasExpired()
                || !disableCause.pointerMatches(information)) {
            if (increase < 1.0) {
                increase = 1.0;
            }
            PlayerViolation playerViolation = new PlayerViolation(
                    time,
                    increase
            );
            boolean event = Config.settings.getBoolean("Important.enable_developer_api");
            CancelCause silentCause = this.executor.getSilentCause();

            Runnable runnable = () -> {
                if (event) {
                    PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(
                            player.getInstance(),
                            hackType,
                            playerViolation.increase,
                            information
                    );
                    Register.manager.callEvent(playerViolationEvent);

                    if (playerViolationEvent.isCancelled()) {
                        return;
                    }
                }
                this.store(player.dataType, playerViolation);
                this.notify(playerViolation, information);
                this.punish();
                this.executor.prevention = newPrevention;
                this.executor.prevention.canPrevent =
                        !hackType.getCheck().isSilent(player.dataType, player.getWorld().getName())
                                && (silentCause == null
                                || silentCause.hasExpired()
                                || !silentCause.pointerMatches(information))
                                && ResearchEngine.getRequiredPlayers(hackType, player.dataType, name, PlayerEvidence.preventionProbability) == 0
                                && this.surpassedProbability(player.dataType, PlayerEvidence.preventionProbability);
            };

            if (!event || SpartanBukkit.isSynchronised()) {
                runnable.run();
            } else {
                SpartanBukkit.transferTask(player, runnable);
            }
        }
    }

    // Notification

    public final boolean canSendNotification(Object detected) {
        long time = System.currentTimeMillis();

        if (this.notifications <= time) {
            boolean player = detected instanceof SpartanPlayer;
            int ticks = this.executor.getNotificationTicksCooldown(
                    player ? (SpartanPlayer) detected : null
            );

            if (ticks > 0) {
                boolean access;

                if (player) {
                    access = ((SpartanPlayer) detected).getExecutor(
                            hackType
                    ).getDetection(
                            this.name
                    ).surpassedProbability(
                            ((SpartanPlayer) detected).dataType,
                            PlayerEvidence.notificationProbability
                    );
                } else {
                    PlayerProfile profile = ResearchEngine.getPlayerProfile(detected.toString(), true);
                    access = profile.getExecutor(
                            hackType
                    ).getDetection(
                            this.name
                    ).surpassedProbability(
                            profile.getLastDataType(),
                            PlayerEvidence.notificationProbability
                    );
                }

                if (access) {
                    this.notifications = time + (ticks * TPS.tickTime);
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private void notify(PlayerViolation playerViolation, String information) {
        double probability = this.getProbability(player.dataType),
                certainty = PlayerEvidence.probabilityToCertainty(probability) * 100.0;
        int roundedCertainty = AlgebraUtils.integerRound(certainty);
        boolean hasSufficientData = ResearchEngine.getRequiredPlayers(hackType, player.dataType, name, PlayerEvidence.notificationProbability) == 0;
        String notification = ConfigUtils.replaceWithSyntax(
                player,
                Config.messages.getColorfulString("detection_notification")
                        .replace("{info}", information)
                        .replace("{vls:detection}", String.valueOf(AlgebraUtils.cut(playerViolation.increase, 2)))
                        .replace("{vls:percentage}",
                                hasSufficientData && roundedCertainty > 0 ? (roundedCertainty + "%") : "unlikely"
                        ),
                hackType
        );

        SpartanLocation location = player.movement.getLocation();
        information = player.getInstance().getName() + " failed " + hackType
                + " (" + CheckExecutor.violationLevelIdentifier + " " + playerViolation.increase + "), "
                + "(Server-Version: " + MultiVersion.serverVersion.toString() + "), "
                + "(Certainty: " + AlgebraUtils.cut(certainty, 2) + "), "
                + "(Plugin-Version: " + API.getVersion() + "), "
                + "(Silent: " + hackType.getCheck().isSilent(player.dataType, player.getWorld().getName()) + "), "
                + "(" + CheckExecutor.javaPlayerIdentifier + " " + (!player.isBedrockPlayer()) + ")" + ", "
                + "(" + CheckExecutor.detectionIdentifier + " " + this.name + ")" + ", "
                + "(Packets: " + player.protocol.packetsEnabled() + "), "
                + "(Ping: " + player.protocol.getPing() + "ms), "
                + "(W-XYZ: " + location.world.getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "), "
                + "[Information: " + information + "]";
        AntiCheatLogs.logInfo(
                player,
                notification,
                information,
                hasSufficientData
                        && PlayerEvidence.surpassedProbability(
                        probability,
                        PlayerEvidence.notificationProbability
                ),
                null,
                hackType,
                playerViolation,
                !hasSufficientData
        );

        // Local Notifications
        String command = Config.settings.getString("Notifications.message_clickable_command")
                .replace("{player}", player.getInstance().getName());

        if (Config.settings.getBoolean("Notifications.individual_only_notifications")) {
            if (DetectionNotifications.isEnabled(player.protocol)) { // Attention
                ClickableMessage.sendCommand(player.getInstance(), notification, command, command);
            }
        } else {
            List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers();

            if (!notificationPlayers.isEmpty()) {
                for (SpartanPlayer staff : notificationPlayers) {
                    if (staff.getExecutor(this.hackType).getDetection(this.name).canSendNotification(this.player)) {
                        ClickableMessage.sendCommand(
                                staff.getInstance(),
                                notification,
                                command,
                                command
                        );
                    }
                }
            }
        }
    }

    // Punishment

    private void punish() {
        Check check = hackType.getCheck();

        if (check.canPunish(player.dataType)) {
            List<String> commands = check.getPunishmentCommands();

            if (!commands.isEmpty()
                    && ResearchEngine.getRequiredPlayers(hackType, player.dataType, name, PlayerEvidence.punishmentProbability) == 0) {
                Collection<Enums.HackType> detectedHacks = player.protocol.getProfile().getEvidenceList(
                        player.dataType,
                        PlayerEvidence.punishmentProbability
                );
                detectedHacks.removeIf(loopHackType -> !loopHackType.getCheck().canPunish(player.dataType));

                if (!detectedHacks.isEmpty()
                        && detectedHacks.contains(hackType)) {
                    int index = 0;
                    boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");
                    StringBuilder stringBuilder = new StringBuilder();

                    for (Enums.HackType detectedHack : detectedHacks) {
                        stringBuilder.append(detectedHack.getCheck().getName()).append(", ");
                    }
                    String detections = stringBuilder.substring(0, stringBuilder.length() - 2);

                    for (String command : commands) {
                        String modifiedCommand = ConfigUtils.replaceWithSyntax(
                                player,
                                command.replaceAll("\\{detections}|\\{detection}", detections),
                                null
                        );
                        commands.set(index++, modifiedCommand);

                        if (enabledDeveloperAPI) {
                            Runnable runnable = () -> {
                                PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(
                                        player.getInstance(),
                                        hackType,
                                        detectedHacks,
                                        modifiedCommand
                                );
                                Register.manager.callEvent(event);

                                if (!event.isCancelled()) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                                }
                            };

                            if (SpartanBukkit.isSynchronised()) {
                                runnable.run();
                            } else {
                                SpartanBukkit.transferTask(player, runnable);
                            }
                        } else {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                        }
                    }
                    SpartanLocation location = player.movement.getLocation();
                    CloudConnections.executeDiscordWebhook(
                            "punishments",
                            player.protocol.getUUID(),
                            player.getInstance().getName(),
                            location.getBlockX(),
                            location.getBlockY(),
                            location.getBlockZ(),
                            "Punishment",
                            StringUtils.toString(commands, "\n")
                    );
                }
            }
        }
    }

    // Prevention

    public final void cancel(String information, double violations, SpartanLocation location,
                             int cancelTicks, boolean groundTeleport, double damage) {
        if (this.isEnabled()) {
            long time = System.currentTimeMillis();

            if (this.executor.canFunction()
                    && !CloudBase.isInformationCancelled(this.hackType, information)) {
                this.violate(
                        new HackPrevention(
                                location,
                                cancelTicks,
                                groundTeleport,
                                damage
                        ),
                        information,
                        violations,
                        time
                );
            }
        }
    }

    public final void cancel(String information, double violations, SpartanLocation location,
                             int cancelTicks, boolean groundTeleport) {
        cancel(information, violations, location, cancelTicks, groundTeleport, 0.0);
    }

    public final void cancel(String information, double violations, SpartanLocation location,
                             int cancelTicks) {
        cancel(information, violations, location, cancelTicks, false, 0.0);
    }

    public final void cancel(String information, double violations, SpartanLocation location) {
        cancel(information, violations, location, 0, false, 0.0);
    }

    public final void cancel(String information, double violations) {
        cancel(information, violations, null, 0, false, 0.0);
    }

    public final void cancel(String information) {
        cancel(information, 1.0, null, 0, false, 0.0);
    }

}
