package com.vagdedes.spartan.objects.system;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.configuration.Config;
import com.vagdedes.spartan.functionality.configuration.AntiCheatLogs;
import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickablemessage.ClickableMessage;
import com.vagdedes.spartan.functionality.performance.FalsePositiveDetection;
import com.vagdedes.spartan.functionality.synchronicity.CrossServerInformation;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import com.vagdedes.spartan.gui.SpartanMenu;
import com.vagdedes.spartan.handlers.identifiers.simple.CheckProtection;
import com.vagdedes.spartan.handlers.stability.Cache;
import com.vagdedes.spartan.handlers.stability.CancelViolation;
import com.vagdedes.spartan.handlers.stability.NotifyViolation;
import com.vagdedes.spartan.handlers.stability.TPS;
import com.vagdedes.spartan.objects.profiling.PlayerViolation;
import com.vagdedes.spartan.objects.replicates.SpartanLocation;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.system.SpartanBukkit;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.server.ConfigUtils;
import com.vagdedes.spartan.utils.server.PluginUtils;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import me.vagdedes.spartan.api.ViolationResetEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class LiveViolation { // todo simplify

    private static final Map<LiveViolation, String> maxLevel
            = Cache.store(Collections.synchronizedMap(new LinkedHashMap<>()));
    public static final String falsePositiveDisclaimer = "§4(False Positive)§f ";

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (!maxLevel.isEmpty()) {
                    synchronized (maxLevel) {
                        Iterator<Map.Entry<LiveViolation, String>> iterator = maxLevel.entrySet().iterator();

                        while (iterator.hasNext()) {
                            Map.Entry<LiveViolation, String> entry = iterator.next();
                            LiveViolation violations = entry.getKey();

                            if (violations.getLevel() < Check.maxViolationsPerCycle) {
                                new HackPrevention(violations.player, violations.hackType, entry.getValue(),
                                        null, 0, false, 0.0, 1);
                            } else {
                                iterator.remove();
                            }
                        }
                    }
                }
            }, 1L, 1L);
        }
    }

    private static class LoopLevelCache {
        private long expiration;
        private boolean result;

        private LoopLevelCache() {
            this.expiration = 0L;
            this.result = false;
        }

        private boolean isValid() {
            return expiration >= System.currentTimeMillis();
        }

        private boolean setResult(boolean result) {
            this.result = result;
            this.expiration = System.currentTimeMillis() + 1_000L;
            return result;
        }
    }

    private final SpartanPlayer player;
    public final Enums.HackType hackType;
    private long cycleExpiration, lastViolation, lastTick;
    private final Map<Integer, Long> cancelledLevel;
    private int level, lastCancelledLevel;
    private final LoopLevelCache cancelledLevelCache;
    private CancelCause disableCause, silentCause;
    private final Map<Integer, HackPrevention> queue;

    public LiveViolation(SpartanPlayer player, Enums.HackType hackType) {
        this.player = player;
        this.hackType = hackType;
        this.level = 0;
        this.cancelledLevel = Collections.synchronizedMap(new LinkedHashMap<>());
        this.lastViolation = 0L;
        this.lastTick = 0L;
        this.lastCancelledLevel = 0;
        this.cancelledLevelCache = new LoopLevelCache();
        this.queue = Collections.synchronizedMap(new LinkedHashMap<>());
        reset(); // Do not make it local as it takes part in object initiation and is not important
    }

    // Separator

    void queue(HackPrevention hackPrevention, Check check) {
        if (queue.size() < check.getDefaultCancelViolation()) {
            synchronized (queue) {
                queue.putIfAbsent(hackPrevention.information.hashCode(), hackPrevention);
            }
        }
    }

    public boolean process() {
        HackPrevention hp;
        boolean processed;

        synchronized (queue) {
            Iterator<HackPrevention> iterator = queue.values().iterator();

            if (iterator.hasNext()) {
                hp = iterator.next();
                processed = hp.processed;

                if (hp.expiration < System.currentTimeMillis()) {
                    iterator.remove();
                } else {
                    hp.processed = true;
                }
            } else {
                hp = null;
                processed = false;
            }
        }

        if (hp != null
                && !CheckProtection.hasCooldown(player)
                && !CloudFeature.isPublicInformationCancelled(hackType, hp.information)
                && (disableCause == null
                || !disableCause.pointerMatches(hp.information))) {
            Check check = hackType.getCheck();
            int violations = this.getLevel();
            boolean canPrevent, canPreventAlternative, falsePositive,
                    hacker = player.getProfile().isHacker(),
                    suspected = !hacker && player.getProfile().isSuspected(hackType),
                    suspectedOrHacker = suspected || hacker,
                    enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");
            PlayerViolationEvent playerViolationEvent;
            PlayerViolation playerViolation = new PlayerViolation(
                    hackType,
                    hp.time,
                    hp.information,
                    violations + 1
            );

            if (suspectedOrHacker) {
                if (silentCause == null || !silentCause.pointerMatches(hp.information)) {
                    canPrevent = true;
                    canPreventAlternative = true;
                } else {
                    canPrevent = false;
                    canPreventAlternative = false;
                }
            } else if (CancelViolation.isForced(player, hackType, this, playerViolation.similarityIdentity)
                    || violations >= CancelViolation.get(hackType, player.getDataType())) {
                if (silentCause == null || !silentCause.pointerMatches(hp.information)) {
                    canPrevent = true;
                    canPreventAlternative = true;
                } else {
                    canPrevent = false;
                    canPreventAlternative = false;
                }
            } else {
                canPrevent = false;
                canPreventAlternative = violations > 0  // Testing scenario
                        && !check.hasMaximumDefaultCancelViolation()
                        && player.getLastViolation().getLastViolationTime(true) <= Check.violationCycleSeconds
                        && !SpartanBukkit.isProductionServer()
                        && DetectionNotifications.isEnabled(player);
            }
            if (FalsePositiveDetection.canCorrect(playerViolation, this)) {
                falsePositive = true;
            } else {
                falsePositive = false;
                violations += 1;
            }

            this.setLastViolationTime(hp.time); // Always after false-positive check

            if (!processed) {
                // API Event
                if (enabledDeveloperAPI) {
                    playerViolationEvent = new PlayerViolationEvent(player.getPlayer(), hackType, violations, hp.information, falsePositive);
                    Register.manager.callEvent(playerViolationEvent);
                } else {
                    playerViolationEvent = null;
                }
            } else {
                playerViolationEvent = null;
            }

            if (playerViolationEvent == null || !playerViolationEvent.isCancelled()) {
                if (falsePositive) {
                    this.removeMaxLevel(hp.information);
                    violations = this.increaseCancelledLevel(playerViolation.similarityIdentity, hacker ? 2.0 : suspected ? 1.5 : 1.0);
                    performNotification(playerViolation.detection, hp.information, violations, false, true, canPrevent || canPreventAlternative, suspectedOrHacker);
                } else {
                    if (processed) {
                        performNotification(playerViolation.detection, hp.information, violations, false, false, canPrevent || canPreventAlternative, suspectedOrHacker);
                    } else {
                        switch (hp.violation) {
                            case 2:
                                this.setMaxLevel(hp.information); // Algorithm will redirect this to case '1'.
                                break;
                            case 1:
                                player.getProfile().getViolationHistory(hackType).store(playerViolation);
                                this.setLevel(playerViolation.similarityIdentity, violations);
                                performNotification(playerViolation.detection, hp.information, violations, true, false, canPrevent || canPreventAlternative, suspectedOrHacker);
                                this.performPunishments(violations);
                                break;
                            default:
                                player.getProfile().getViolationHistory(hackType).store(playerViolation);
                                performNotification(playerViolation.detection, hp.information, violations, false, false, canPrevent || canPreventAlternative, suspectedOrHacker);
                                break;
                        }
                    }

                    if (!check.isSilent(player.getWorld().getName(), player)
                            && (canPrevent || canPreventAlternative)) {
                        if (enabledDeveloperAPI) {
                            CheckCancelEvent checkCancelEvent = new CheckCancelEvent(player.getPlayer(), hackType);
                            Register.manager.callEvent(checkCancelEvent);

                            if (!checkCancelEvent.isCancelled()) {
                                hp.handle(player, hackType);
                                return true;
                            }
                        } else {
                            hp.handle(player, hackType);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Separator

    public int getLevel() {
        timeReset();
        return level;
    }

    public int getCancelledLevel(int hash) {
        Long time;

        synchronized (cancelledLevel) {
            time = cancelledLevel.get(hash);
        }
        if (time == null) {
            return 0;
        } else {
            time -= System.currentTimeMillis();
            return time > 0L
                    ? Math.min(AlgebraUtils.integerCeil(time / 1000.0), Check.maxViolationsPerCycle)
                    : 0;
        }
    }

    public int getLastCancelledLevel() {
        return lastCancelledLevel;
    }

    public boolean hasLevel() {
        return getLevel() > 0;
    }

    public boolean hasCancelledLevel() {
        if (cancelledLevelCache.isValid()) {
            return cancelledLevelCache.result;
        } else if (!cancelledLevel.isEmpty()) {
            synchronized (cancelledLevel) {
                Iterator<Long> iterator = cancelledLevel.values().iterator();
                long current = System.currentTimeMillis();

                while (iterator.hasNext()) {
                    if (iterator.next() > current) {
                        return cancelledLevelCache.setResult(true);
                    } else {
                        iterator.remove();
                    }
                }
            }
        }
        return cancelledLevelCache.setResult(false);
    }

    public boolean hasMaxCancelledLevel(int hash) {
        Long time;

        synchronized (cancelledLevel) {
            time = cancelledLevel.get(hash);
        }
        if (time != null) {
            time -= System.currentTimeMillis();
            return time > 0L
                    && (time / 1000.0) >= (hackType.getCheck().getMaxCancelledViolations(player.getDataType(), hash) - 1); // -1 so we don't ceil the number
        } else {
            return false;
        }
    }

    public void setLevel(int hash, int amount) {
        timeReset();
        int previousLevel = level;

        // Violations
        if (amount < Check.maxViolationsPerCycle) {
            this.level = amount;
            increaseCancelledLevel(
                    hash,
                    this.player.getProfile().isHacker() ? 2.0
                            : this.player.getProfile().isSuspected(hackType) ? 1.5
                            : 1.0
            ); // Increase it in case a player has reached the max level and is still cheating
        }
        this.lastCancelledLevel = 0;
        timeReset(); // Always after changing the level

        // Always last
        if (previousLevel != level) {
            SpartanMenu.playerInfo.refresh(player.getName());
        }
    }

    public int increaseCancelledLevel(int hash, double multiplier) {
        this.lastCancelledLevel = hash;

        synchronized (cancelledLevel) {
            Long time = cancelledLevel.get(hash);

            if (time == null) {
                cancelledLevel.put(hash, System.currentTimeMillis() + AlgebraUtils.integerRound(1_000 * multiplier));
                return 1;
            } else {
                long current = System.currentTimeMillis();

                if (time < current) {
                    this.cancelledLevel.put(hash, current + AlgebraUtils.integerRound(1_000 * multiplier));
                    return 1;
                } else {
                    time += AlgebraUtils.integerRound(1_000 * multiplier);
                    this.cancelledLevel.put(hash, time);
                    return AlgebraUtils.integerCeil(time / 1000.0);
                }
            }
        }
    }

    public void setMaxLevel(String message) {
        synchronized (maxLevel) {
            maxLevel.put(this, message);
        }
    }

    public void removeMaxLevel(String message) {
        synchronized (maxLevel) {
            String cached = maxLevel.get(this);

            if (cached != null && cached.equals(message)) {
                maxLevel.remove(this);
            }
        }
    }

    // Separator

    public void reset() {
        reset(false);
    }

    private void reset(boolean local) {
        ViolationResetEvent event;

        if (SpartanBukkit.isSynchronised()) {
            Player player = this.player.getPlayer();

            if (player != null
                    && player.isOnline()) {
                if (Config.settings.getBoolean("Important.enable_developer_api")) {
                    event = new ViolationResetEvent(player, this.hackType);
                    Register.manager.callEvent(event);
                } else {
                    event = null;
                }
            } else {
                event = null;
            }
        } else {
            event = null;
        }

        if (event == null || !event.isCancelled()) {
            synchronized (maxLevel) {
                maxLevel.remove(this);
            }
            this.level = 0;
            this.lastCancelledLevel = 0;
        } else if ((System.currentTimeMillis() - this.cycleExpiration) > Check.violationCycleSeconds) { // Forceful reset to keep potential developer API usage within hardcoded thresholds
            synchronized (maxLevel) {
                maxLevel.remove(this);
            }
            this.level = 0;
            this.lastCancelledLevel = 0;
        }
        this.cycleExpiration = System.currentTimeMillis() + Check.violationCycleSeconds;

        // Always last
        if (player != null) {
            player.getProfile().getEvidence().remove(hackType, true, false, true);

            if (local) {
                SpartanMenu.playerInfo.refresh(player.getName());
            }
        }
    }

    private void timeReset() {
        long time = System.currentTimeMillis();

        if (this.cycleExpiration < time) {
            reset(true);
        }
    }

    // Separator

    public long getLastTick() {
        return lastTick;
    }

    public long getLastViolationTime(boolean maxIfNull) {
        return maxIfNull && this.lastViolation == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - this.lastViolation;
    }

    public void setLastViolationTime(long time) {
        this.lastViolation = time;
        this.lastTick = TPS.getTick(player);
        this.player.setLastViolation(this);
    }

    // Separator

    public CancelCause getDisableCause() {
        return disableCause != null && !disableCause.hasExpired() ? disableCause : null;
    }

    public CancelCause getSilentCause() {
        return silentCause != null && !silentCause.hasExpired() ? silentCause : null;
    }

    public void addDisableCause(String reason, String pointer, int ticks) {
        if (disableCause != null) {
            disableCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            disableCause = new CancelCause(reason, pointer, ticks);
        }
        SpartanMenu.playerInfo.refresh(player.getName());
    }

    public void addSilentCause(String reason, String pointer, int ticks) {
        if (silentCause != null) {
            silentCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            silentCause = new CancelCause(reason, pointer, ticks);
        }
        SpartanMenu.playerInfo.refresh(player.getName());
    }

    public void removeDisableCause() {
        this.disableCause = null;
        SpartanMenu.playerInfo.refresh(player.getName());
    }

    public void removeSilentCause() {
        this.silentCause = null;
        SpartanMenu.playerInfo.refresh(player.getName());
    }

    // Separator

    private void performPunishments(int violation) {
        Check check = hackType.getCheck();

        if (check.canPunish()) {
            boolean legacy = Config.isLegacy();
            Collection<String> commands = legacy
                    ? check.getLegacyCommands(violation)
                    : Config.settings.getPunishmentCommands();

            if (!commands.isEmpty()) {
                Player n = player.getPlayer();

                if (n != null && n.isOnline()) {
                    boolean performed = false, found = false;

                    if (legacy) {
                        boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");

                        for (String command : commands) {
                            if (command != null) {
                                found = true;
                                String modifiedCommand = ConfigUtils.replaceWithSyntax(player, command, hackType);

                                if (enabledDeveloperAPI) {
                                    PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(n, hackType, modifiedCommand);
                                    Register.manager.callEvent(event);

                                    if (event.isCancelled()) {
                                        continue;
                                    }
                                }
                                performed = true;
                                SpartanBukkit.runDelayedTask(player, () -> {
                                    if (player != null) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), modifiedCommand);
                                    }
                                }, 1);
                            }
                        }
                    } else {
                        Collection<Enums.HackType> detectedHacks = player.getProfile().getEvidence().calculate(player, hackType);
                        detectedHacks.removeIf(loopHackType -> !loopHackType.getCheck().canPunish());

                        if (!detectedHacks.isEmpty()) {
                            boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api");
                            StringBuilder stringBuilder = new StringBuilder();

                            for (Enums.HackType detectedHack : detectedHacks) {
                                stringBuilder.append(detectedHack.getCheck().getName()).append(", ");
                            }
                            String detections = stringBuilder.substring(0, stringBuilder.length() - 2);

                            for (String command : commands) {
                                if (command != null) {
                                    found = true;
                                    String modifiedCommand = ConfigUtils.replaceWithSyntax(
                                            player,
                                            command.replaceAll("\\{detections}|\\{detection}", detections),
                                            null
                                    );

                                    if (enabledDeveloperAPI) {
                                        PlayerViolationCommandEvent event = new PlayerViolationCommandEvent(n, hackType, detectedHacks, modifiedCommand);
                                        Register.manager.callEvent(event);

                                        if (event.isCancelled()) {
                                            continue;
                                        }
                                    }

                                    performed = true;
                                    SpartanBukkit.runDelayedTask(player, () -> {
                                        if (player != null) {
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
                            player.getProfile().getPunishmentHistory().increasePunishments(player, commandsString);
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

    private void performNotification(String detailedDetection,
                                     String info,
                                     int level,
                                     boolean log,
                                     boolean falsePositive,
                                     boolean canPrevent,
                                     boolean hacker) {
        if (level < Check.maxViolationsPerCycle) {
            // Variables
            boolean individualOnlyNotifications = Config.settings.getBoolean("Notifications.individual_only_notifications");
            int cancelViolation = CancelViolation.get(hackType, player.getDataType()),
                    playerCount = SpartanBukkit.getPlayerCount();

            // Message Preparation
            String shorterMessage, message = Config.messages.getColorfulString("detection_notification");

            if (ConfigUtils.contains(message, "{info}")) {
                if (falsePositive) {
                    message = falsePositiveDisclaimer + ConfigUtils.replaceWithSyntax(player, message, hackType);
                } else {
                    message = ConfigUtils.replaceWithSyntax(player, message, hackType);
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
                    message = falsePositiveDisclaimer + ConfigUtils.replaceWithSyntax(player, message, hackType);
                } else {
                    message = ConfigUtils.replaceWithSyntax(player, message, hackType);
                }
                shorterMessage = message;
            }

            // Cross-Server & Console Notifications
            boolean cannotPunish = !hackType.getCheck().supportsLiveEvidence(),
                    consoleLog = !falsePositive && level >= cancelViolation,
                    important = hacker
                            || cannotPunish
                            || canPrevent
                            || consoleLog
                            || player.getProfile().isSuspected();

            if (important) {
                CrossServerInformation.queueNotification(message, true);
            }

            // Logs
            if (log) {
                SpartanLocation location = player.getLocation();
                String cancelViolationString = (canPrevent ? "-" : "") + cancelViolation + (hackType.getCheck().hasCancelViolation() ? "*" : ""),
                        information = (falsePositive ? "(False Positive) " : "")
                                + Config.getConstruct() + player.getName() + " failed " + hackType + " (VL: " + level
                                + ") " + "[(Version: " + MultiVersion.fork() + " " + MultiVersion.versionString()
                                + "), (C-V: " + cancelViolationString + ") (Silent: "
                                + hackType.getCheck().isSilent(player.getWorld().getName(), player) + "), "
                                + "(Ping: " + player.getPing() + "ms), (TPS: "
                                + AlgebraUtils.cut(TPS.get(player, false), 3) + "), (Hacker: " + hacker
                                + "), (Online: " + playerCount + "), " +
                                "(XYZ: " + location.getBlockX() + " " + location.getBlockY() + " "
                                + location.getBlockZ() + "), (" + info + ")]";
                AntiCheatLogs.logInfo(player, information, consoleLog ? shorterMessage : null, null, hackType, true, level);
            }

            // Local Notifications
            String command = Config.settings.getString("Notifications.message_clickable_command").replace("{player}", player.getName());

            if (individualOnlyNotifications) {
                Integer divisor = DetectionNotifications.getDivisor(player, false);

                if (DetectionNotifications.canAcceptMessages(player, divisor, false)
                        && isDivisorValid(level, divisor)) { // Attention
                    ClickableMessage.sendCommand(player, message, "Command: " + command, command);
                }
            } else {
                List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers(false);

                if (!notificationPlayers.isEmpty()) {
                    for (SpartanPlayer staff : notificationPlayers) {
                        int divisor = NotifyViolation.get(staff, player, cancelViolation, playerCount);

                        if (isDivisorValid(level, divisor)) {
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

    private boolean isDivisorValid(int level, int divisor) {
        return divisor == 0 || level % divisor == 0;
    }
}
