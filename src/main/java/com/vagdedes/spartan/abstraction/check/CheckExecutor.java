package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.configuration.implementation.Compatibility;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.profiling.PlayerEvidence;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import com.vagdedes.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import com.vagdedes.spartan.compatibility.manual.enchants.EcoEnchants;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.*;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.server.ConfigUtils;
import me.vagdedes.spartan.api.CheckCancelEvent;
import me.vagdedes.spartan.api.PlayerViolationCommandEvent;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;

import java.util.Collection;
import java.util.List;

public abstract class CheckExecutor extends CheckDetection {

    private static final boolean v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8);
    public static final String
            violationLevelIdentifier = "Violations:",
            javaPlayerIdentifier = "Java:",
            detectionIdentifier = "Detection:";

    private final long creation;
    private long notifications;
    private CancelCause disableCause, silentCause;
    HackPrevention prevention;
    private boolean cancelled;

    public CheckExecutor(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.creation = System.currentTimeMillis();
        this.prevention = new HackPrevention();
    }

    // Run

    public final void run(boolean cancelled) {
        this.cancelled = cancelled;
        this.runInternal(cancelled);
    }

    protected void runInternal(boolean cancelled) {

    }

    // Handle

    public final void handle(boolean cancelled, Object object) {
        this.cancelled = cancelled;
        this.handleInternal(cancelled, object);
    }

    protected void handleInternal(boolean cancelled, Object object) {

    }

    // Separator

    protected boolean canRun() {
        return true;
    }

    // Separator

    final boolean canFunction() {
        return (System.currentTimeMillis() - this.creation) > TPS.maximum * TPS.tickTime
                && (!cancelled || hackType.getCheck().handleCancelledEvents)
                && !player.protocol.isLoading()
                && (!v1_8 || player.getInstance().getGameMode() != GameMode.SPECTATOR)
                && hackType.getCheck().isEnabled(player.dataType, player.getWorld().getName())
                && Attributes.getAmount(player, Attributes.GENERIC_SCALE) == 0.0
                && canRun()
                && !Permissions.isBypassing(player, hackType);
    }

    // Causes

    public final CancelCause getDisableCause() {
        if (disableCause == null || disableCause.hasExpired()) {
            return MythicMobs.is(player)
                    ? new CancelCause(Compatibility.CompatibilityType.MYTHIC_MOBS)
                    : ItemsAdder.is(player)
                    ? new CancelCause(Compatibility.CompatibilityType.ITEMS_ADDER)
                    : CustomEnchantsPlus.has(player)
                    ? new CancelCause(Compatibility.CompatibilityType.CUSTOM_ENCHANTS_PLUS)
                    : EcoEnchants.has(player)
                    ? new CancelCause(Compatibility.CompatibilityType.ECO_ENCHANTS)
                    : null;
        } else {
            return disableCause;
        }
    }

    public final CancelCause getSilentCause() {
        return silentCause != null && silentCause.hasExpired() ? null : silentCause;
    }

    public final void addDisableCause(String reason, String pointer, int ticks) {
        if (reason == null) {
            reason = this.hackType.getCheck().getName();
        }
        if (disableCause != null) {
            disableCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            disableCause = new CancelCause(reason, pointer, ticks);
        }
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    public final void addSilentCause(String reason, String pointer, int ticks) {
        if (silentCause != null) {
            silentCause.merge(new CancelCause(reason, pointer, ticks));
        } else {
            silentCause = new CancelCause(reason, pointer, ticks);
        }
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    public final void removeDisableCause() {
        this.disableCause = null;
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    public final void removeSilentCause() {
        this.silentCause = null;
        InteractiveInventory.playerInfo.refresh(player.getInstance().getName());
    }

    // Prevention

    public final boolean prevent() {
        if (this.prevention.complete()) {
            if (SpartanBukkit.isSynchronised()) {
                if (player.protocol.packetsEnabled()) {
                    return false;
                } else {
                    CheckCancelEvent checkCancelEvent;

                    if (Config.settings.getBoolean("Important.enable_developer_api")) {
                        checkCancelEvent = new CheckCancelEvent(player.getInstance(), hackType);
                        Register.manager.callEvent(checkCancelEvent);
                    } else {
                        checkCancelEvent = null;
                    }

                    if (checkCancelEvent == null
                            || !checkCancelEvent.isCancelled()) {
                        this.prevention.handle(player);
                        return true;
                    } else {
                        return false;
                    }
                }
            } else if (!player.protocol.packetsEnabled()) {
                Thread thread = Thread.currentThread();
                Boolean[] cancelled = new Boolean[1];

                SpartanBukkit.transferTask(this.player, () -> {
                    CheckCancelEvent checkCancelEvent = new CheckCancelEvent(player.getInstance(), hackType);
                    Register.manager.callEvent(checkCancelEvent);
                    cancelled[0] = checkCancelEvent.isCancelled();

                    synchronized (thread) {
                        thread.notifyAll();
                    }
                });
                synchronized (thread) {
                    if (cancelled[0] == null) {
                        try {
                            thread.wait();
                        } catch (Exception ex) {
                        }
                    }
                }
                if (!cancelled[0]) {
                    this.prevention.handle(player);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // Punishment

    void punish() {
        Check check = hackType.getCheck();

        if (check.canPunish(player.dataType)) {
            List<String> commands = check.getPunishmentCommands();

            if (!commands.isEmpty()
                    && ResearchEngine.getRequiredPlayers(hackType, PlayerEvidence.punishmentProbability) == 0) {
                Collection<Enums.HackType> detectedHacks = player.protocol.getProfile().evidence.getKnowledgeList(
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

    // Notification

    private int getNotificationTicksCooldown(SpartanPlayer detected) {
        Integer frequency = DetectionNotifications.getFrequency(this.player);

        if (frequency != null
                && frequency != DetectionNotifications.defaultFrequency) {
            return frequency;
        } else if (detected != null
                && (detected.getInstance().equals(this.player.getInstance())
                || detected.getWorld().equals(this.player.getWorld())
                && detected.movement.getLocation().distance(this.player.movement.getLocation()) <= PlayerUtils.chunk)) {
            return AlgebraUtils.integerRound(Math.sqrt(TPS.maximum));
        } else {
            return AlgebraUtils.integerCeil(TPS.maximum);
        }
    }

    public final boolean canSendNotification(Object detected) {
        long time = System.currentTimeMillis();

        if (this.notifications <= time) {
            boolean player = detected instanceof SpartanPlayer;
            int ticks = this.getNotificationTicksCooldown(
                    player ? (SpartanPlayer) detected : null
            );

            if (ticks > 0) {
                if (player
                        ? ((SpartanPlayer) detected).protocol.getProfile().evidence.surpassed(this.hackType, PlayerEvidence.notificationProbability)
                        : ResearchEngine.getPlayerProfile(detected.toString(), true).evidence.surpassed(this.hackType, PlayerEvidence.notificationProbability)) {
                    this.notifications = time + (ticks * TPS.tickTime);
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

}
