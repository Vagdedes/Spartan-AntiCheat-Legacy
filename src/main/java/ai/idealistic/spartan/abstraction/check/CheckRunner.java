package ai.idealistic.spartan.abstraction.check;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.compatibility.Compatibility;
import ai.idealistic.spartan.compatibility.manual.abilities.ItemsAdder;
import ai.idealistic.spartan.compatibility.manual.building.MythicMobs;
import ai.idealistic.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import ai.idealistic.spartan.compatibility.manual.enchants.EcoEnchants;
import ai.idealistic.spartan.compatibility.manual.vanilla.Attributes;
import ai.idealistic.spartan.compatibility.necessary.protocollib.ProtocolLib;
import ai.idealistic.spartan.functionality.connection.PluginAddons;
import ai.idealistic.spartan.functionality.moderation.DetectionNotifications;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.Permissions;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.functionality.server.TPS;
import ai.idealistic.spartan.functionality.tracking.DetectionCharge;
import ai.idealistic.spartan.utils.math.AlgebraUtils;
import ai.idealistic.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.event.Cancellable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CheckRunner extends CheckProcess {

    private static final boolean v1_8 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8);

    final long creation;
    private final Collection<CheckCancellation> disableCauses, silentCauses;
    private boolean cancelled;
    private final Map<String, CheckDetection> detections;

    public CheckRunner(CheckEnums.HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.creation = System.currentTimeMillis();
        this.detections = new ConcurrentHashMap<>(2);
        this.disableCauses = Collections.synchronizedList(new ArrayList<>(1));
        this.silentCauses = Collections.synchronizedList(new ArrayList<>(1));
    }

    // Detections

    public final CheckDetection getDetection(String detection) {
        return this.detections.get(detection);
    }

    public final Collection<CheckDetection> getDetections() {
        return this.detections.values();
    }

    protected final CheckDetection addDetection(String name, CheckDetection detection) {
        return this.detections.putIfAbsent(name, detection);
    }

    public final void removeDetection(CheckDetection detection) {
        this.detections.remove(detection.name);
    }

    // Handle

    public final void handle(Object cancelled, Object object) {
        boolean result;

        if (cancelled == null) {
            if (object instanceof Cancellable) {
                result = ((Cancellable) object).isCancelled();
            } else {
                result = false;
            }
        } else if (cancelled instanceof Boolean) {
            result = (Boolean) cancelled;
        } else {
            if (cancelled instanceof Cancellable) {
                result = ((Cancellable) cancelled).isCancelled();
            } else {
                result = false;
            }
        }
        this.cancelled = result;
        this.handleInternal(result, object);
    }

    protected void handleInternal(boolean cancelled, Object object) {

    }

    // Separator

    protected boolean canRun() {
        return true;
    }

    // Separator

    final boolean canCall() {
        StringBuilder builder = new StringBuilder();

        if (this.protocol.npc) {
            builder.append("Player was determined to be an NPC");
        }
        if (!DetectionCharge.has()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Detections need to be charged, run /")
                    .append(Register.pluginName.toLowerCase())
                    .append(" charge");
        }
        String world = this.protocol.getWorld().getName();

        if (!hackType.getCheck().isEnabled(this.protocol.getDataType(), world)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Check is disabled in the world '")
                    .append(world)
                    .append("' or player's edition");
        }
        if (cancelled && !hackType.getCheck().handleCancelledEvents) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Event is cancelled and check does not handle cancelled events");
        }
        if (v1_8 && this.protocol.getGameMode() == GameMode.SPECTATOR) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Player is in spectator mode");
        }
        if (Attributes.getAmount(this.protocol, Attributes.GENERIC_SCALE) != Double.MIN_VALUE) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Player has a scale attribute applied");
        }
        if (!PluginAddons.ownsCheck(this.hackType)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Check '")
                    .append(this.hackType.getCheck().getName())
                    .append("' is not owned");
        }
        if (!PluginAddons.ownsEdition(this.protocol.getDataType())) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Edition '")
                    .append(this.protocol.getDataType().toString())
                    .append("' of the check '")
                    .append(this.hackType.getCheck().hackType.toString())
                    .append("' is not owned");
        }
        if (builder.length() > 0) {
            this.addMomentDisableCause(builder.toString());
            return false;
        } else {
            return true;
        }
    }

    final boolean canCancel() {
        StringBuilder builder = new StringBuilder();

        if ((System.currentTimeMillis() - this.creation) <= TPS.maximum * TPS.tickTime) {
            builder.append("Check ran too quickly after creation");
        }
        if (ProtocolLib.isTemporary(this.protocol.bukkit())) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Player is temporary");
        }
        if (Permissions.isBypassing(this.protocol.bukkit(), hackType)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("Player is bypassing");
        }
        if (builder.length() > 0) {
            this.addMomentDisableCause(builder.toString());
            return false;
        } else {
            return true;
        }
    }

    public final List<String> getEvidence() {
        if (this.protocol == null) {
            return new ArrayList<>();
        }
        List<String> evidence = new ArrayList<>();

        for (CheckDetection detection : this.getDetections()) {
            if (detection.getLevel(this.protocol.getDataType())
                    >= detection.getNotificationLevel(this.protocol.getDataType())) {
                evidence.add(detection.name);
            }
        }
        return evidence;
    }

    // Causes

    private CheckCancellation getLastCause(Collection<CheckCancellation> collection) {
        Iterator<CheckCancellation> iterator = collection.iterator();

        while (iterator.hasNext()) {
            CheckCancellation cause = iterator.next();

            if (cause.hasExpired()) {
                iterator.remove();
            } else {
                return cause;
            }
        }
        return null;
    }

    public final CheckCancellation getDisableCause() {
        CheckCancellation disableCause = this.getLastCause(this.disableCauses);

        if (disableCause == null) {
            return MythicMobs.is(this.protocol)
                    ? new CheckCancellation(Compatibility.CompatibilityType.MYTHIC_MOBS)
                    : ItemsAdder.is(this.protocol)
                    ? new CheckCancellation(Compatibility.CompatibilityType.ITEMS_ADDER)
                    : CustomEnchantsPlus.has(this.protocol)
                    ? new CheckCancellation(Compatibility.CompatibilityType.CUSTOM_ENCHANTS_PLUS)
                    : EcoEnchants.has(this.protocol)
                    ? new CheckCancellation(Compatibility.CompatibilityType.ECO_ENCHANTS)
                    : null;
        } else {
            return disableCause;
        }
    }

    public final CheckCancellation getSilentCause() {
        return this.getLastCause(this.silentCauses);
    }

    public final void addMomentDisableCause(String reason) {
        this.disableCauses.add(new CheckCancellation(reason, null, -1));
        PluginBase.playerInfo.refresh(this.protocol.bukkit().getName());
    }

    public final void addDisableCause(String reason, String pointer, int ticks) {
        if (reason == null) {
            reason = this.hackType.getCheck().getName();
        }
        this.disableCauses.add(new CheckCancellation(reason, pointer, ticks));
        PluginBase.playerInfo.refresh(this.protocol.bukkit().getName());
    }

    public final void addSilentCause(String reason, String pointer, int ticks) {
        if (reason == null) {
            reason = this.hackType.getCheck().getName();
        }
        this.silentCauses.add(new CheckCancellation(reason, pointer, ticks));
        PluginBase.playerInfo.refresh(this.protocol.bukkit().getName());
    }

    public final void removeDisableCause() {
        this.disableCauses.clear();
        PluginBase.playerInfo.refresh(this.protocol.bukkit().getName());
    }

    public final void removeSilentCause() {
        this.silentCauses.clear();
        PluginBase.playerInfo.refresh(this.protocol.bukkit().getName());
    }

    // Prevention

    public final boolean prevent() {
        if (!this.detections.isEmpty()) {
            for (CheckDetection detection : this.getDetections()) {
                if (detection.prevent()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Notification

    final int getNotificationTicksCooldown(PlayerProtocol detected) {
        Integer frequency = DetectionNotifications.getFrequency(this.protocol);

        if (frequency != null
                && frequency != DetectionNotifications.defaultFrequency) {
            return frequency;
        } else if (detected != null
                && (detected.equals(this.protocol)
                || detected.getWorld().equals(this.protocol.getWorld())
                && detected.getLocation().distance(this.protocol.getLocation()) <= PlayerUtils.chunk)) {
            return AlgebraUtils.integerRound(Math.sqrt(TPS.maximum));
        } else {
            return AlgebraUtils.integerRound(TPS.maximum);
        }
    }

}
