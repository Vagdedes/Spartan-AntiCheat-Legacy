package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.InformationAnalysis;
import com.vagdedes.spartan.abstraction.profiling.PlayerViolation;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CrossServerInformation;
import com.vagdedes.spartan.functionality.identifiers.simple.CheckDelay;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.management.Cache;
import com.vagdedes.spartan.functionality.management.Config;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.notifications.clickable.ClickableMessage;
import com.vagdedes.spartan.functionality.performance.NotifyViolation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.AntiCheatLogs;
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

public class LiveViolation {

    private static final Map<LiveViolation, PlayerViolation> maxLevel
            = Cache.store(Collections.synchronizedMap(new LinkedHashMap<>()));

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (!maxLevel.isEmpty()) {
                    synchronized (maxLevel) {
                        Iterator<Map.Entry<LiveViolation, PlayerViolation>> iterator = maxLevel.entrySet().iterator();

                        while (iterator.hasNext()) {
                            Map.Entry<LiveViolation, PlayerViolation> entry = iterator.next();
                            LiveViolation violations = entry.getKey();

                            if (violations.getLevel(entry.getValue().identity) < Check.maxViolationsPerCycle) {
                                new HackPrevention(violations.player, violations.hackType, entry.getValue().information,
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

    private final SpartanPlayer player;
    private final Enums.HackType hackType;
    private long lastTick;
    private final Map<Integer, Long> level;
    private CancelCause disableCause, silentCause;
    private final Map<Integer, HackPrevention> queue;

    public LiveViolation(SpartanPlayer player, Enums.HackType hackType) {
        this.player = player;
        this.hackType = hackType;
        this.level = Collections.synchronizedMap(new LinkedHashMap<>());
        this.lastTick = 0L;
        this.queue = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    // Separator

    void queue(HackPrevention newPrevention) {
        if (this.queue.size() < Check.standardIgnoredViolations) {
            synchronized (this.queue) {
                this.queue.putIfAbsent(newPrevention.information.hashCode(), newPrevention);
            }
        } else {
            synchronized (this.queue) {
                Iterator<HackPrevention> iterator = this.queue.values().iterator();

                if (iterator.hasNext()) {
                    HackPrevention prevention = iterator.next();

                    if (prevention.processed && prevention.stay <= newPrevention.stay) {
                        iterator.remove();
                        this.queue.putIfAbsent(newPrevention.information.hashCode(), newPrevention);
                    }
                }
            }
        }
    }

    public boolean process() {
        HackPrevention hp;
        boolean processed;

        synchronized (this.queue) {
            Iterator<HackPrevention> iterator = this.queue.values().iterator();

            if (iterator.hasNext()) {
                hp = iterator.next();
                processed = hp.processed;

                if (hp.expiration < TPS.getTick(player)) {
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
                && !CheckDelay.hasCooldown(player)
                && !CloudBase.isPublicInformationCancelled(hackType, hp.information)
                && (disableCause == null
                || !disableCause.pointerMatches(hp.information))) {
            Check check = hackType.getCheck();
            InformationAnalysis analysis = new InformationAnalysis(hackType, hp.information);
            int violations = this.getLevel(analysis.identity) + 1;
            PlayerViolation playerViolation = new PlayerViolation(
                    hp.time,
                    hackType,
                    hp.information,
                    violations,
                    analysis
            );
            boolean enabledDeveloperAPI = Config.settings.getBoolean("Important.enable_developer_api"),
                    canPrevent = !check.isSilent(player.getWorld().getName())
                            && (silentCause == null
                            || !silentCause.pointerMatches(hp.information))
                            && (player.getProfile().isSuspectedOrHacker(hackType)
                            || violations >= playerViolation.getIgnoredViolations(player));

            this.lastTick = hp.tick;
            this.player.setLastViolation(this);

            if (processed) {
                performNotification(hp.information, playerViolation, false, canPrevent);
            } else {
                switch (hp.violation) {
                    case 2:
                        // Algorithm will redirect this to case '1'.
                        synchronized (maxLevel) {
                            maxLevel.put(this, playerViolation);
                        }
                        break;
                    case 1:
                        if (enabledDeveloperAPI) {
                            PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(player.getPlayer(), hackType, violations, hp.information);
                            Register.manager.callEvent(playerViolationEvent);

                            if (playerViolationEvent.isCancelled()) {
                                break;
                            }
                        }
                        player.getProfile().getViolationHistory(hackType).store(playerViolation);
                        this.increaseLevel(playerViolation.identity);
                        performNotification(hp.information, playerViolation, true, canPrevent);

                        if (canPrevent) {
                            this.performPunishments(playerViolation);
                        }
                        break;
                    default:
                        if (enabledDeveloperAPI) {
                            PlayerViolationEvent playerViolationEvent = new PlayerViolationEvent(player.getPlayer(), hackType, violations, hp.information);
                            Register.manager.callEvent(playerViolationEvent);

                            if (playerViolationEvent.isCancelled()) {
                                break;
                            }
                        }
                        player.getProfile().getViolationHistory(hackType).store(playerViolation);
                        performNotification(hp.information, playerViolation, false, canPrevent);
                        break;
                }
            }

            if (canPrevent) {
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
        return false;
    }

    // Separator

    public int getLevel(int hash) {
        Long time;

        synchronized (this.level) {
            time = this.level.get(hash);
        }
        return this.calculateLevel(time);
    }

    private int calculateLevel(Long time) {
        if (time != null) {
            time -= System.currentTimeMillis();
            return time > 0L ? (int) (time / Check.violationTimeWorth) : 0;
        } else {
            return 0;
        }
    }

    public int getTotalLevel() {
        if (!this.level.isEmpty()) {
            int total = 0;

            for (Long level : this.level.values()) {
                total += this.calculateLevel(level);
            }
            return total;
        } else {
            return 0;
        }
    }

    public boolean hasLevel() {
        if (!this.level.isEmpty()) {
            for (Long level : this.level.values()) {
                if (this.calculateLevel(level) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void increaseLevel(int hash) {
        int multiplier = 1 + player.getProfile().evidence.getKnowledgeList().size();

        synchronized (this.level) {
            Long time = this.level.get(hash);

            if (time == null) {
                this.level.put(hash, System.currentTimeMillis() + (Check.violationTimeWorth * multiplier));
            } else {
                long current = System.currentTimeMillis();

                if (time < current) {
                    this.level.put(hash, current + (Check.violationTimeWorth * multiplier));
                } else {
                    time += Check.violationTimeWorth * multiplier;
                    this.level.put(hash, time);
                }
            }
        }
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    // Separator

    public void reset() {
        ViolationResetEvent event;

        if (SpartanBukkit.isSynchronised()) {
            Player player = this.player.getPlayer();

            if (player != null && player.isOnline()) {
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
            synchronized (this.level) {
                this.level.clear();
            }
        }

        // Always last
        player.getProfile().evidence.remove(hackType, true, false, true);
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    // Separator

    public boolean isDetected(boolean prevention) {
        return lastTick == TPS.getTick(player)
                && (!prevention
                || !hackType.getCheck().isSilent(player.getWorld().getName()));
    }

    public boolean wasDetected(boolean prevention) {
        return lastTick >= (TPS.getTick(player) - 1)
                && (!prevention
                || !hackType.getCheck().isSilent(player.getWorld().getName()));
    }

    public long getLastTick() {
        return lastTick;
    }

    // Separator

    public CancelCause getDisableCause() {
        return disableCause;
    }

    public CancelCause getSilentCause() {
        return silentCause;
    }

    public void addDisableCause(String reason, String pointer, int ticks) {
        if (disableCause != null) {
            disableCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            disableCause = new CancelCause(reason, pointer, ticks);
        }
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    public void addSilentCause(String reason, String pointer, int ticks) {
        if (silentCause != null) {
            silentCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            silentCause = new CancelCause(reason, pointer, ticks);
        }
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    public void removeDisableCause() {
        this.disableCause = null;
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    public void removeSilentCause() {
        this.silentCause = null;
        InteractiveInventory.playerInfo.refresh(player.name);
    }

    // Separator

    private void performPunishments(PlayerViolation playerViolation) {
        Check check = hackType.getCheck();

        if (check.canPunish) {
            boolean legacy = Config.isLegacy();
            Collection<String> commands = legacy
                    ? check.getLegacyCommands(playerViolation.level)
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
                        Collection<Enums.HackType> detectedHacks = player.getProfile().evidence.calculate(player, playerViolation);
                        detectedHacks.removeIf(loopHackType -> !loopHackType.getCheck().canPunish);

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
                            player.getProfile().punishmentHistory.increasePunishments(player, commandsString);
                        }
                    } else if (found && AwarenessNotifications.canSend(AwarenessNotifications.uuid, hackType + "-cancelled-punishment-event")) {
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
            } else if (AwarenessNotifications.canSend(AwarenessNotifications.uuid, hackType + "-no-punishment-commands")) {
                AwarenessNotifications.forcefullySend("Just a reminder that you have set no punishment commands for the '" + hackType + "' check.");
            }
        }
    }

    private void performNotification(String info,
                                     PlayerViolation playerViolation,
                                     boolean log,
                                     boolean canPrevent) {
        if (playerViolation.level < Check.maxViolationsPerCycle) {
            boolean individualOnlyNotifications = Config.settings.getBoolean("Notifications.individual_only_notifications");
            int ignoredViolations = playerViolation.getIgnoredViolations(player);
            String message = ConfigUtils.replaceWithSyntax(
                    player,
                    Config.messages.getColorfulString("detection_notification")
                            .replace("{info}", info),
                    hackType
            );

            if (canPrevent
                    || !hackType.getCheck().supportsLiveEvidence) {
                CrossServerInformation.queueNotification(message, true);
            }
            if (log) {
                SpartanLocation location = player.movement.getLocation();
                String information = Config.getConstruct() + player.name + " failed " + hackType + " (VL: " + playerViolation
                        + ") " + "[(Version: " + MultiVersion.fork() + " " + MultiVersion.versionString()
                        + "), (I-V: " + (canPrevent ? "-" : "") + ignoredViolations + ") (Silent: "
                        + hackType.getCheck().isSilent(player.getWorld().getName()) + "), "
                        + "(Ping: " + player.getPing() + "ms), (TPS: "
                        + AlgebraUtils.cut(TPS.get(player, false), 3) + "), " +
                        "(XYZ: " + location.getBlockX() + " " + location.getBlockY() + " "
                        + location.getBlockZ() + "), (" + info + ")]";
                AntiCheatLogs.logInfo(player, information, information, null, hackType, true, playerViolation.level);
            }

            // Local Notifications
            String command = Config.settings.getString("Notifications.message_clickable_command")
                    .replace("{player}", player.name);

            if (individualOnlyNotifications) {
                Integer divisor = DetectionNotifications.getDivisor(player, false);

                if (DetectionNotifications.canAcceptMessages(player, divisor, false)
                        && isDivisorValid(playerViolation.level, divisor)) { // Attention
                    ClickableMessage.sendCommand(player, message, "Command: " + command, command);
                }
            } else {
                List<SpartanPlayer> notificationPlayers = DetectionNotifications.getPlayers(false);

                if (!notificationPlayers.isEmpty()) {
                    for (SpartanPlayer staff : notificationPlayers) {
                        if (isDivisorValid(
                                playerViolation.level,
                                NotifyViolation.get(staff, player, ignoredViolations)
                        )) {
                            ClickableMessage.sendCommand(
                                    staff,
                                    message,
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
