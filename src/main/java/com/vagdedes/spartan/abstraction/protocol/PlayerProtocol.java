package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.data.CheckBoundData;
import com.vagdedes.spartan.abstraction.data.EnvinromentData;
import com.vagdedes.spartan.abstraction.data.PacketWorld;
import com.vagdedes.spartan.abstraction.data.TimerBalancer;
import com.vagdedes.spartan.abstraction.data.component.ComponentXZ;
import com.vagdedes.spartan.abstraction.data.component.ComponentY;
import com.vagdedes.spartan.abstraction.event.CPlayerVelocityEvent;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.necessary.BedrockCompatibility;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.minecraft.entity.AxisAlignedBB;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PotionEffectUtils;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerProtocol {

    public long activeCreationTime;
    private Player bukkit;
    @Getter
    private EnvinromentData environment;
    public final PlayerBukkit bukkitExtra;
    public final PlayerPunishments punishments;
    public final PlayerTrackers trackers;
    public int
            slime0Tick,
            rightClickCounter,
            transactionVl,
            flyingTicks,
            predictedSlimeTicks;
    public final boolean npc;
    public boolean
            onGround,
            onGroundFrom,
            pistonTick,
            transactionBoot,
            clickBlocker,
            transactionLocal,
            transactionSentKeep,
            useItemPacket,
            useItemPacketReset;
    @Setter
    private Location
            location,
            from,
            teleport;
    public String fromWorld;
    private final List<Location> positionHistory;
    public CPlayerVelocityEvent claimedVelocity;
    public final List<CPlayerVelocityEvent>
            claimedVeloGravity,
            claimedVeloSpeed;
    public long tickTime;
    public final MultiVersion.MCVersion version;
    public final TimerBalancer timerBalancer;
    public final Check.DetectionType detectionType;
    @Getter
    private Check.DataType dataType;
    private Set<AxisAlignedBB> axisMatrixCache;
    @Setter
    private CheckBoundData checkBoundData;
    public final PacketWorld packetWorld;
    public PlayerTickEvent lastTickEvent;
    public short transactionId;
    public long
            transactionTime,
            transactionLastTime,
            transactionPing,
            lagTick,
            oldClickTime,
            soulSandWater = 0L,
            magmaCubeWater = 0L,
            lastVelocity = 0L;
    public boolean entityHandle;
    private ComponentY componentY;
    private ComponentXZ componentXZ;
    private Vector clampVector;
    private final Map<Long, SpartanLocation> locations;
    Location schedulerFrom;

    public PlayerProtocol(Player player) {
        long time = System.currentTimeMillis();
        this.activeCreationTime = time;
        this.bukkit = player;
        this.version = MultiVersion.get(player);
        this.packetWorld = new PacketWorld(player);
        this.npc = player.getAddress() == null;
        this.punishments = new PlayerPunishments(this);
        this.dataType = BedrockCompatibility.isPlayer(player)
                ? Check.DataType.BEDROCK
                : Check.DataType.JAVA;
        this.detectionType = this.packetsEnabled()
                ? Check.DetectionType.PACKETS
                : Check.DetectionType.BUKKIT;
        this.onGround = false;
        this.onGroundFrom = false;
        this.location = ProtocolTools.getLoadLocation(player);
        this.from = null;
        this.fromWorld = "";
        this.teleport = null;
        this.transactionVl = 0;
        this.bukkitExtra = new PlayerBukkit(this);
        this.timerBalancer = new TimerBalancer();
        this.flyingTicks = 0;
        this.oldClickTime = System.currentTimeMillis();
        this.clickBlocker = false;
        this.tickTime = time;
        this.environment = new EnvinromentData();
        this.rightClickCounter = 0;
        this.positionHistory = Collections.synchronizedList(new LinkedList<>());
        this.claimedVelocity = null;
        this.claimedVeloGravity = new CopyOnWriteArrayList<>();
        this.claimedVeloSpeed = new CopyOnWriteArrayList<>();
        this.entityHandle = false;
        this.axisMatrixCache = new HashSet<>();
        this.checkBoundData = null;
        this.pistonTick = false;
        this.lastTickEvent = null;
        this.transactionId = (short) -1939;
        this.transactionTime = System.currentTimeMillis();
        this.transactionLastTime = System.currentTimeMillis();
        this.transactionPing = 0;
        this.transactionBoot = false;
        this.transactionLocal = false;
        this.transactionSentKeep = false;
        this.lagTick = 0;
        this.predictedSlimeTicks = 0;
        this.componentY = new ComponentY();
        this.componentXZ = new ComponentXZ();
        this.useItemPacket = false;
        this.useItemPacketReset = false;
        this.trackers = new PlayerTrackers();
        this.slime0Tick = 0;
        this.clampVector = new Vector();
        this.locations = Collections.synchronizedMap(new LinkedHashMap<>());
        this.schedulerFrom = this.location;

        // Always last
        this.profile().update(this);
    }

    static {
        PluginBase.runRepeatingTask(() -> {
            Collection<PlayerProtocol> protocols = PluginBase.getProtocols();

            if (!protocols.isEmpty()) {
                for (PlayerProtocol protocol : protocols) {
                    Check.DataType previousDataType = protocol.dataType;
                    protocol.dataType = BedrockCompatibility.isPlayer(protocol.bukkit())
                            ? Check.DataType.BEDROCK
                            : Check.DataType.JAVA;

                    if (protocol.dataType != previousDataType) {
                        protocol.profile().update(protocol);
                    }
                    if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                        protocol.bukkitExtra.setStoredPotionEffects();
                    }
                    if (protocol.bukkitExtra.maxNearbyEntitiesCoordinate[0] > 0.0
                            || protocol.bukkitExtra.maxNearbyEntitiesCoordinate[1] > 0.0
                            || protocol.bukkitExtra.maxNearbyEntitiesCoordinate[2] > 0.0) {
                        protocol.bukkitExtra.nearbyEntities = protocol.bukkit().getNearbyEntities(
                                protocol.bukkitExtra.maxNearbyEntitiesCoordinate[0],
                                protocol.bukkitExtra.maxNearbyEntitiesCoordinate[1],
                                protocol.bukkitExtra.maxNearbyEntitiesCoordinate[2]
                        ).toArray(new Entity[0]);
                    }
                    protocol.bukkitExtra.checkForAFK();
                    protocol.profile().executeRunners(false, null);
                    protocol.schedulerFrom = protocol.getLocation();
                }
            }
        }, 1L, 1L);
    }

    // Separator

    public final Player bukkit() {
        return this.bukkit;
    }

    public void updateBukkit(Player player) {
        this.bukkit = player;
    }

    // Separator

    void resetActiveCreationTime() {
        this.activeCreationTime = System.currentTimeMillis();
    }

    public long getActiveTimePlayed() {
        return System.currentTimeMillis() - this.activeCreationTime;
    }

    // Separator

    public long getTimePassed(long time) {
        return System.currentTimeMillis() - this.activeCreationTime;
    }

    public boolean isUsingVersion(MultiVersion.MCVersion trialVersion) {
        return version.ordinal() == trialVersion.ordinal();
    }

    // Separator

    public boolean isDesync() {
        return this.transactionSentKeep && (System.currentTimeMillis() - this.transactionTime > 55);
    }

    public boolean isBlatantDesync() {
        return this.transactionSentKeep && (System.currentTimeMillis() - this.transactionTime > 150);
    }

    public boolean isSDesync() {
        return this.transactionSentKeep && (System.currentTimeMillis() - this.transactionTime > 400);
    }

    // Separator

    public boolean isUsingVersionOrGreater(MultiVersion.MCVersion trialVersion) {
        return version.ordinal() >= trialVersion.ordinal();
    }

    // Separator

    public Location getLocation() {
        Location loc = this.packetsEnabled()
                ? this.location
                : ProtocolLib.getLocationOrNull(this.bukkit());
        return loc != null
                ? loc
                : SpartanLocation.bukkitDefault.clone();
    }

    public Location getFromLocation() {
        return this.from != null
                ? this.from
                : this.getLocation();
    }

    public Location getVehicleLocation() {
        Entity vehicle = this.bukkitExtra.getVehicle();

        if (vehicle instanceof LivingEntity || vehicle instanceof Vehicle) {
            return ProtocolLib.getLocationOrNull(vehicle);
        } else {
            return null;
        }
    }

    public Location getLocationOrVehicle() {
        Location vehicleLocation = getVehicleLocation();

        if (vehicleLocation == null) {
            return this.getLocation();
        } else {
            return vehicleLocation;
        }
    }

    // Separator

    public List<Location> getPositionHistory() {
        synchronized (this.positionHistory) {
            return new ArrayList<>(this.positionHistory);
        }
    }

    public SpartanLocation getPastTickRotation() {
        Location l = this.getLocation().clone();
        l.setYaw(this.from.getYaw());
        l.setPitch(this.from.getPitch());
        return new SpartanLocation(l);
    }

    // Separator

    public boolean teleport(Location location) {
        if (location.getX() == SpartanLocation.bukkitDefault.getX()
                && location.getY() == SpartanLocation.bukkitDefault.getY()
                && location.getZ() == SpartanLocation.bukkitDefault.getZ()) {
            return false;
        }
        boolean sync = PluginBase.isSynchronised();

        if (sync) {
            this.bukkit().leaveVehicle();
        }

        if (MultiVersion.folia) {
            this.bukkit().teleportAsync(location);
        } else if (sync) {
            this.bukkit().teleport(location);
        } else {
            PluginBase.transferTask(
                    this,
                    () -> {
                        Player bukkit = this.bukkit();

                        if (bukkit.isOnline()) {
                            bukkit.teleport(location);
                        }
                    }
            );
        }
        return true;
    }

    public int getPing() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return this.bukkit().isOnline() ? this.bukkit().getPing() : 0;
        } else {
            try {
                Object obj = PluginBase.getCraftPlayerMethod(this.bukkit(), "ping");
                return obj instanceof Integer ? Math.max((int) obj, 0) : 0;
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    public UUID getUUID() {
        return ProtocolLib.isTemporary(this.bukkit())
                ? UUID.randomUUID()
                : this.bukkit().getUniqueId();
    }

    public PlayerProfile profile() {
        return ResearchEngine.getPlayerProfile(this);
    }

    // Separator

    public void setFromLocation(Location location) {
        this.from = location;
    }

    public void addRawLocation(Location location) {
        synchronized (this.positionHistory) {
            this.positionHistory.add(location.clone());

            if (this.positionHistory.size() > 20) {
                Iterator<Location> iterator = this.positionHistory.iterator();
                iterator.next();
                iterator.remove();
            }
        }
    }

    public World getWorld() {
        return this.getLocation().getWorld();
    }

    // Separator

    public boolean packetsEnabled() {
        return PluginBase.packetsEnabled() && !this.isBedrockPlayer();
    }

    public boolean isBedrockPlayer() {
        return this.dataType == Check.DataType.BEDROCK;
    }

    // Separator

    public boolean isOnGroundFrom() {
        return packetsEnabled()
                ? this.onGroundFrom
                : this.bukkitExtra.isOnGround();
    }

    // Separator

    public boolean processLastMoveEvent(
            Location originalTo,
            Location vehicle,
            SpartanLocation to,
            Location from,
            boolean packets
    ) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                || this.isUsingVersionOrGreater(MultiVersion.MCVersion.V1_17)) {
            double x = clampMin(to.getX(), -3.0E7D, 3.0E7D),
                    y = clampMin(to.getY(), -2.0E7D, 2.0E7D),
                    z = clampMin(to.getZ(), -3.0E7D, 3.0E7D);
            org.bukkit.util.Vector vector = new Vector(x, y, z);
            double d = this.clampVector.distanceSquared(vector);
            this.clampVector = vector;

            if (d < 4e-8) {
                return false;
            }
        }
        if (vehicle == null) {
            Location known = this.getLocation();

            if (known.getX() != originalTo.getX()
                    || known.getY() != originalTo.getY()
                    || known.getZ() != originalTo.getZ()
                    || known.getYaw() != originalTo.getYaw()
                    || known.getPitch() != originalTo.getPitch()) {
                SpartanLocation spartanLocation = new SpartanLocation(originalTo);

                synchronized (this.locations) {
                    if (this.locations.size() == TPS.maximum) {
                        Iterator<Long> iterator = this.locations.keySet().iterator();
                        iterator.next();
                        iterator.remove();
                    }
                    this.locations.put(System.currentTimeMillis(), spartanLocation);
                }
            }
        }
        if (!packets) {
            this.setFromLocation(from);
        }
        return true;
    }

    private double clampMin(double d, double d2, double d3) {
        return d < d2 ? d2 : Math.min(d, d3);
    }

    // Separator

    public Location getSchedulerFromLocation() {
        return schedulerFrom;
    }

    public List<SpartanLocation> getLocations() {
        return new ArrayList<>(this.locations.values());
    }

    public Set<Map.Entry<Long, SpartanLocation>> getLocationEntries() {
        synchronized (this.locations) {
            return new HashSet<>(locations.entrySet());
        }
    }

    // Separator

    public boolean isJumping(double d) {
        return PlayerUtils.isJumping(
                d,
                PlayerUtils.getPotionLevel(this, PotionEffectUtils.JUMP) + 1,
                GroundUtils.maxHeightLengthRatio
        );
    }

    public boolean justJumped(double d) {
        return PlayerUtils.justJumped(
                d,
                PlayerUtils.getPotionLevel(this, PotionEffectUtils.JUMP) + 1,
                GroundUtils.maxHeightLengthRatio
        );
    }

    public boolean isLowEyeHeight() {
        return this.bukkitExtra.getEyeHeight() < 1.0;
    }

}