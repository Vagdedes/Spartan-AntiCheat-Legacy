package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.Compatibility;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.Config;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.math.MathHelper;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import com.vagdedes.spartan.utils.minecraft.server.PluginUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerBukkit {

    final PlayerProtocol protocol;
    private final Map<PotionEffectType, ExtendedPotionEffect> potionEffects;
    private long
            lastInteraction = 0L,
            lastFlight = 0L,
            lastGlide = 0L;
    private boolean afk;
    Entity[] nearbyEntities;
    final double[] maxNearbyEntitiesCoordinate;
    public boolean
            onGround = false,
            sprinting = false,
            sneaking = false;
    @Setter
    Location location;

    // Object

    public PlayerBukkit(PlayerProtocol protocol) {
        this.protocol = protocol;
        this.potionEffects = new ConcurrentHashMap<>(2);
        this.setStoredPotionEffects();
        this.lastInteraction = System.currentTimeMillis();
        this.afk = false;
        this.nearbyEntities = new Entity[0];
        this.maxNearbyEntitiesCoordinate = new double[3];
        this.location = ProtocolTools.getLoadLocation(protocol.bukkit());
    }

    public void setLastInteraction() {
        this.lastInteraction = System.currentTimeMillis();
    }

    void checkForAFK() {
        long lastInteraction = System.currentTimeMillis() - this.lastInteraction;

        if (lastInteraction >= 20_000L) {
            if (!afk) {
                this.protocol.profile().getContinuity().setActiveTime(
                        System.currentTimeMillis(),
                        this.protocol.getActiveTimePlayed() - lastInteraction,
                        true
                );
                this.afk = true;
            }
        } else if (this.afk) {
            this.protocol.resetActiveCreationTime();
            this.afk = false;
        }
    }

    public boolean isAFK() {
        return this.afk || this.protocol.npc;
    }

    @Override
    public String toString() {
        return this.protocol.bukkit().getName();
    }

    public boolean debug(boolean function, boolean broadcast, boolean cutDecimals, Object... message) {
        if (function) {
            if (this.protocol.bukkit().isWhitelisted()) {
                String string = "§f" + this.protocol.bukkit().getName() + " ";

                if (message == null || message.length == 0) {
                    string += new Random().nextInt();
                } else {
                    int i = 0;

                    if (cutDecimals) {
                        for (Object object : message) {
                            if (object instanceof Double) {
                                message[i] = ((i + 1) % 2 == 0 ? "§c" : "§7")
                                        + AlgebraUtils.cut((double) object, GroundUtils.maxHeightLength);
                            } else {
                                message[i] = ((i + 1) % 2 == 0 ? "§c" : "§7") + object;
                            }
                            i++;
                        }
                    } else {
                        for (Object object : message) {
                            message[i] = ((i + 1) % 2 == 0 ? "§c" : "§7") + object;
                            i++;
                        }
                    }
                    string += StringUtils.toString(message, " ");
                }
                Collection<PlayerProtocol> protocols = PluginBase.getProtocols();

                if (!protocols.isEmpty()) {
                    if (broadcast) {
                        Bukkit.broadcastMessage(string);
                    } else {
                        for (PlayerProtocol protocol : protocols) {
                            if (protocol.bukkit().isOp()) {
                                protocol.bukkit().sendMessage(string);
                            }
                        }
                    }
                }
                Bukkit.getConsoleSender().sendMessage(string);
                return true;
            }
        }
        return false;
    }

    // Separator

    public int getMaxChatLength() {
        return this.protocol.isBedrockPlayer() ? 512 :
                MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)
                        || this.protocol.isUsingVersionOrGreater(MultiVersion.MCVersion.V1_11)
                        || PluginUtils.exists("viaversion")
                        || Compatibility.CompatibilityType.PROTOCOL_SUPPORT.isFunctional() ? 256 :
                        100;
    }

    public void sendImportantMessage(String message) {
        this.protocol.bukkit().sendMessage("");
        this.protocol.bukkit().sendMessage(message);
        this.protocol.bukkit().sendMessage("");
    }

    // Separator

    public Inventory createInventory(int size, String title) {
        return Bukkit.createInventory(this.protocol.bukkit(), size, title);
    }

    // Separator

    public ItemStack getItemInHand() {
        return this.protocol.bukkit().getInventory().getItemInHand();
    }

    // Separator

    public Location getLocation() {
        Location loc = this.protocol.packetsEnabled()
                ? this.location
                : ProtocolLib.getLocationOrNull(this.protocol.bukkit());
        return loc != null
                ? loc
                : SpartanLocation.bukkitDefault.clone();
    }

    public World getWorld() {
        return this.getLocation().getWorld();
    }

    // Separator

    public boolean isOnGround() {
        if (this.protocol.packetsEnabled()) {
            return this.protocol.onGround;
        } else {
            Entity vehicle = this.protocol.bukkitExtra.getVehicle();

            if (vehicle != null) {
                return vehicle.isOnGround();
            } else {
                return this.protocol.bukkit().isOnGround();
            }
        }
    }

    public void setOnGround(boolean isOnGround) {
        this.protocol.onGroundFrom = this.onGround;
        this.onGround = isOnGround;
    }

    // Separator

    public SpartanLocation getTargetBlock(double distance, double limit) {
        for (int i = 0; i < AlgebraUtils.integerCeil(Math.min(distance, limit)); i++) {
            SpartanLocation location = new SpartanLocation(
                    protocol.bukkitExtra.getLocation().clone().add(
                            0,
                            this.getEyeHeight(),
                            0
                    ).add(
                            protocol.bukkitExtra.getLocation().getDirection().multiply(i)
                    )
            );

            if (BlockUtils.isFullSolid(location.getBlock().getType())) {
                return location.getBlockLocation();
            }
        }
        return null;
    }

    // Separator

    public boolean isFrozen() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return this.protocol.bukkit().isFrozen();
        } else {
            return false;
        }
    }

    // Separator

    public float getAttackCooldown() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return this.protocol.bukkit().getAttackCooldown();
        } else {
            return 1.0f;
        }
    }

    // Separator

    public Entity getVehicle() {
        return ProtocolLib.getVehicle(this.protocol.bukkit());
    }

    // Separator

    public double getHealth() {
        return ProtocolLib.isTemporary(this.protocol.bukkit())
                ? 0.0
                : this.protocol.bukkit().getHealth();
    }

    // Separator

    public int getEntityId() {
        return ProtocolLib.isTemporary(this.protocol.bukkit())
                ? new Random().nextInt()
                : this.protocol.bukkit().getEntityId();
    }

    // Separator

    void setStoredPotionEffects() {
        if (!ProtocolLib.isTemporary(this.protocol.bukkit())) {
            PluginBase.transferTask(
                    protocol,
                    () -> {
                        for (PotionEffect effect : this.protocol.bukkit().getActivePotionEffects()) {
                            this.potionEffects.put(effect.getType(), new ExtendedPotionEffect(effect));
                        }
                    }
            );
        }
    }

    public Collection<PotionEffect> getActivePotionEffects() {
        return ProtocolLib.isTemporary(this.protocol.bukkit())
                ? new ArrayList<>(0)
                : this.protocol.bukkit().getActivePotionEffects();
    }

    public ExtendedPotionEffect getPotionEffect(PotionEffectType type, long lastActive) {
        this.setStoredPotionEffects();
        ExtendedPotionEffect potionEffect = this.potionEffects.get(type);

        if (potionEffect != null
                && potionEffect.timePassed() <= lastActive
                && potionEffect.bukkitEffect.getType().equals(type)) {
            return potionEffect;
        } else {
            return null;
        }
    }

    public boolean hasPotionEffect(PotionEffectType type, long lastActive) {
        this.setStoredPotionEffects();
        ExtendedPotionEffect potionEffect = this.potionEffects.get(type);
        return potionEffect != null
                && potionEffect.timePassed() <= lastActive
                && potionEffect.bukkitEffect.getType().equals(type);
    }

    // Separator

    public List<Entity> getNearbyEntities(double radius) {
        this.maxNearbyEntitiesCoordinate[0] = Math.max(radius, this.maxNearbyEntitiesCoordinate[0]);
        this.maxNearbyEntitiesCoordinate[1] = Math.max(radius, this.maxNearbyEntitiesCoordinate[1]);
        this.maxNearbyEntitiesCoordinate[2] = Math.max(radius, this.maxNearbyEntitiesCoordinate[2]);

        if (PluginBase.isSynchronised()) {
            List<Entity> list = this.protocol.bukkit().getNearbyEntities(radius, radius, radius);
            this.nearbyEntities = list.toArray(new Entity[0]);
            return list;
        } else if (this.nearbyEntities.length == 0) {
            return new ArrayList<>(0);
        } else {
            List<Entity> entities = new ArrayList<>();
            Location location = protocol.bukkitExtra.getLocation();
            int smallX = MathHelper.floor_double((location.getX() - radius) / PlayerUtils.chunk);
            int bigX = MathHelper.floor_double((location.getX() + radius) / PlayerUtils.chunk);
            int smallZ = MathHelper.floor_double((location.getZ() - radius) / PlayerUtils.chunk);
            int bigZ = MathHelper.floor_double((location.getZ() + radius) / PlayerUtils.chunk);
            double radiusSquared = radius * radius;

            for (int xx = smallX; xx <= bigX; xx++) {
                for (int zz = smallZ; zz <= bigZ; zz++) {
                    for (Entity entity : nearbyEntities) {
                        Location eLoc = ProtocolLib.getLocationOrNull(entity);

                        if (eLoc != null) {
                            if (SpartanLocation.distanceSquared(
                                    eLoc,
                                    location
                            ) <= radiusSquared) {
                                entities.add(entity);
                            }
                        }
                    }
                }
            }
            entities.remove(this.protocol.bukkit());
            return entities;
        }
    }

    public List<Entity> getNearbyEntities(double x, double y, double z) {
        this.maxNearbyEntitiesCoordinate[0] = Math.max(x, this.maxNearbyEntitiesCoordinate[0]);
        this.maxNearbyEntitiesCoordinate[1] = Math.max(y, this.maxNearbyEntitiesCoordinate[1]);
        this.maxNearbyEntitiesCoordinate[2] = Math.max(z, this.maxNearbyEntitiesCoordinate[2]);

        if (PluginBase.isSynchronised()) {
            List<Entity> list = this.protocol.bukkit().getNearbyEntities(x, y, z);
            this.nearbyEntities = list.toArray(new Entity[0]);
            return list;
        } else if (this.nearbyEntities.length == 0) {
            return new ArrayList<>(0);
        } else {
            List<Entity> entities = new ArrayList<>();
            Location location = protocol.bukkitExtra.getLocation();
            int smallX = MathHelper.floor_double((location.getX() - x) / PlayerUtils.chunk);
            int bigX = MathHelper.floor_double((location.getX() + x) / PlayerUtils.chunk);
            int smallZ = MathHelper.floor_double((location.getZ() - z) / PlayerUtils.chunk);
            int bigZ = MathHelper.floor_double((location.getZ() + z) / PlayerUtils.chunk);

            for (int xx = smallX; xx <= bigX; xx++) {
                for (int zz = smallZ; zz <= bigZ; zz++) {
                    for (Entity entity : nearbyEntities) {
                        Location eLoc = ProtocolLib.getLocationOrNull(entity);

                        if (eLoc != null) {
                            if (Math.abs(eLoc.getX() - location.getX()) <= x
                                    && Math.abs(eLoc.getY() - location.getY()) <= y
                                    && Math.abs(eLoc.getZ() - location.getZ()) <= z) {
                                entities.add(entity);
                            }
                        }
                    }
                }
            }
            entities.remove(this.protocol.bukkit());
            return entities;
        }
    }

    // Teleport

    public void groundTeleport() {
        Location location = this.protocol.bukkitExtra.getLocation();
        SpartanLocation locationP1 = new SpartanLocation(location.clone().add(0, 1, 0));

        if (BlockUtils.isSolid(locationP1.getBlock().getType())
                && !(BlockUtils.areWalls(locationP1.getBlock().getType())
                || BlockUtils.canClimb(locationP1.getBlock().getType(), false))) {
            return;
        }
        World world = this.protocol.bukkitExtra.getWorld();
        double startY = Math.min(BlockUtils.getMaxHeight(world), location.getY());
        int iterations = 0;

        for (double progressiveY = startY; startY > BlockUtils.getMinHeight(world); progressiveY--) {
            SpartanLocation loopLocation = new SpartanLocation(
                    location.clone().add(0.0, -(startY - progressiveY), 0.0)
            );
            Material material = loopLocation.getBlock().getType();

            if (iterations != PlayerUtils.chunk
                    && (BlockUtils.canClimb(material, false)
                    || BlockUtils.areWalls(material)
                    || !BlockUtils.isSolid(material))) {
                iterations++;
            } else {
                loopLocation.setY(Math.floor(progressiveY) + 1.0);

                if (this.protocol.packetsEnabled()) {
                    this.protocol.teleport(loopLocation.bukkit());
                }
                break;
            }
        }

        if (iterations > 0) {
            this.protocol.bukkit().setFallDistance(0.0f);

            if (Config.settings.getBoolean("Detections.fall_damage_on_teleport")
                    && iterations > PlayerUtils.fallDamageAboveBlocks) { // Damage
                damage(Math.max(this.protocol.bukkit().getFallDistance(), iterations));
            }
        }
    }

    // Separator

    public void damage(double amount) {
        this.protocol.bukkit().damage(amount);
    }

    // Separator

    public boolean isOutsideOfTheBorder(double deviation) {
        Location loc = protocol.bukkitExtra.getLocation();
        WorldBorder border = this.protocol.bukkitExtra.getWorld().getWorldBorder();
        double size = (border.getSize() / 2.0) + deviation;
        Location center = border.getCenter();
        return Math.abs(loc.getX() - center.getX()) > size
                || Math.abs(loc.getZ() - center.getZ()) > size;
    }

    // Separator

    public double getEyeHeight() {
        return ProtocolLib.getEyeHeight(this.protocol.bukkit());
    }

    // Separator

    public boolean isFlying() {
        Entity vehicle = this.getVehicle();
        boolean flying;

        if (vehicle != null) {
            flying = vehicle instanceof Player && ((Player) vehicle).isFlying();
        } else {
            flying = this.protocol.bukkit().isFlying();
        }
        if (flying) {
            this.lastFlight = System.currentTimeMillis();
        }
        return flying;
    }

    public boolean wasFlying() {
        return this.isFlying()
                || System.currentTimeMillis() - this.lastFlight <= TPS.maximum * TPS.tickTime;
    }

    // Separator

    public boolean isGliding() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            if (this.protocol.bukkit().isGliding()) {
                this.lastGlide = System.currentTimeMillis();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean wasGliding() {
        return this.isGliding()
                || System.currentTimeMillis() - this.lastGlide <= TPS.maximum * TPS.tickTime;
    }

    // Separator

    public boolean isSprinting() {
        return this.protocol.packetsEnabled()
                ? this.sprinting
                : this.protocol.bukkit().isSprinting();
    }

    public boolean isSneaking() {
        return this.protocol.packetsEnabled()
                ? this.sneaking
                : this.protocol.bukkit().isSneaking();
    }

    // Separator

    public boolean isSwimming() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            return this.protocol.bukkit().isSwimming();
        } else {
            return false;
        }
    }

}
