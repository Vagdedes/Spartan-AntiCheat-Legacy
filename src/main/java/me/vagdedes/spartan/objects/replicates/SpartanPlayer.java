package me.vagdedes.spartan.objects.replicates;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import me.vagdedes.spartan.compatibility.manual.building.MythicMobs;
import me.vagdedes.spartan.compatibility.manual.enchants.CustomEnchantsPlus;
import me.vagdedes.spartan.compatibility.manual.enchants.EcoEnchants;
import me.vagdedes.spartan.compatibility.manual.essential.ViaVersion;
import me.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import me.vagdedes.spartan.compatibility.necessary.bedrock.BedrockCompatibility;
import me.vagdedes.spartan.configuration.Compatibility;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.performance.DetectionTick;
import me.vagdedes.spartan.features.performance.MaximumCheckedPlayers;
import me.vagdedes.spartan.features.protections.LagLeniencies;
import me.vagdedes.spartan.features.protections.PlayerLimitPerIP;
import me.vagdedes.spartan.features.protections.Teleport;
import me.vagdedes.spartan.features.synchronicity.SpartanEdition;
import me.vagdedes.spartan.gui.info.PlayerInfo;
import me.vagdedes.spartan.handlers.connection.Latency;
import me.vagdedes.spartan.handlers.identifiers.Damage;
import me.vagdedes.spartan.handlers.identifiers.ElytraUse;
import me.vagdedes.spartan.handlers.identifiers.Velocity;
import me.vagdedes.spartan.handlers.stability.FromLocation;
import me.vagdedes.spartan.handlers.stability.ResearchEngine;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.handlers.stability.TestServer;
import me.vagdedes.spartan.handlers.tracking.ClickData;
import me.vagdedes.spartan.handlers.tracking.CombatProcessing;
import me.vagdedes.spartan.listeners.EventsHandler7;
import me.vagdedes.spartan.objects.data.Timer;
import me.vagdedes.spartan.objects.data.*;
import me.vagdedes.spartan.objects.profiling.PlayerFight;
import me.vagdedes.spartan.objects.profiling.PlayerOpponent;
import me.vagdedes.spartan.objects.profiling.PlayerProfile;
import me.vagdedes.spartan.objects.system.hackPrevention.HackPreventionUtils;
import me.vagdedes.spartan.system.*;
import me.vagdedes.spartan.utils.gameplay.*;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpartanPlayer {

    private static final int tickFrequency = 5 * 1200;
    private static int syncTicks = 0;
    private static final Map<UUID, List<Entity>> worldEntities = new ConcurrentHashMap<>(Math.max(Bukkit.getWorlds().size(), 1));

    final String name;
    private final UUID uuid;
    private final String ipAddress;
    private boolean flying;
    private boolean allowFlight;
    private boolean dead;
    private boolean sleeping;
    private WeatherType playerWeather;
    private PotionEffect[] potionEffects;
    private float walkSpeed;
    private float flySpeed;
    private SpartanInventory inventory;
    private boolean gliding;
    private boolean swimming;
    private boolean sprinting;
    private boolean sneaking;
    private boolean frozen;
    private double eyeHeight;
    private GameMode gameMode;
    private double health;
    private boolean invulnerable;
    private final boolean bedrockPlayer;
    private final ResearchEngine.DataType dataType;
    private final InetSocketAddress address;
    private final long creationTime;
    private final long lastPlayed;
    private final long firstPlayed;
    private boolean usingItem;
    private int fireTicks;
    private double lastDamage;
    private EntityDamageEvent lastDamageCause;
    private SpartanOpenInventory openInventory;
    private int ping;
    private int maximumNoDamageTicks;
    private float fallDistance;
    private int foodLevel;
    private World world;
    private boolean onGroundCustom;
    private long lastHeadMovement;

    // Data

    private final Buffer[] buffer;
    private final Timer[] timer;
    private final Decimals[] decimals;
    private final Tracker[] tracker;
    private final Cooldowns[] cooldowns;
    private final Handlers handlers;

    // Cache

    private PlayerProfile playerProfile;
    private final DetectionTick detectionTick;
    private final ClickData clickData;

    private SpartanLocation location;
    private long cachedLocation;
    private long lastLocation;
    private long chasing;

    List<Entity> nearbyEntities;
    private double nearbyEntitiesDistance;

    private final boolean[] runChecks;
    private final Map<Enums.HackType, Boolean> runCheck;
    private final Map<Enums.HackType, Boolean> runCheckAccountLag;

    // Custom

    private double
            nmsDistance, nmsHorizontalDistance, nmsVerticalDistance,
            customDistance, customHorizontalDistance, customVerticalDistance;
    private long
            lastOffSprint,
            lastFall,
            lastJump, oldLastJump,
            walking, jumpWalking,
            sprintingTime, jumpSprinting,
            swimmingTime,
            flyingTime,
            lastLiquidTime;
    private int
            airTicks, oldAirTicks,
            groundTicks,
            extraPackets;

    static {
        if (Register.isPluginLoaded()) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (MultiVersion.folia) {
                    run(false);
                } else {
                    Threads.executeIfFreeElseHere(SpartanBukkit.playerThread, () -> run(false));
                }
                run(true);
            }, 1L, 1L);
        }
    }

    // Separator

    public static void clear() {
        worldEntities.clear();
        SpartanLocation.memory.clear();
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (players.size() > 0) {
            for (SpartanPlayer p : players) {
                for (Buffer buffer : p.buffer) {
                    buffer.clear();
                }
                for (Timer timer : p.timer) {
                    timer.clear();
                }
                for (Decimals decimals : p.decimals) {
                    decimals.clear();
                }
                for (Tracker tracker : p.tracker) {
                    tracker.clear();
                }
                for (Cooldowns cooldowns : p.cooldowns) {
                    cooldowns.clear();
                }
                p.runCheck.clear();
                p.runCheckAccountLag.clear();
            }
        }
    }

    public static void cacheEntity(Entity entity) {
        List<Entity> collection = worldEntities.get(entity.getWorld().getUID());

        if (collection != null) {
            collection.add(entity);
        }
    }

    private static void run(boolean sync) {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (sync) {
            boolean clearCache, hasPlayers = players.size() > 0;

            if (syncTicks == 0) {
                clearCache = true;
                syncTicks = tickFrequency;
                worldEntities.clear();

                if (hasPlayers) {
                    for (World world : Bukkit.getWorlds()) {
                        worldEntities.put(world.getUID(), world.getEntities());
                    }
                }
            } else {
                clearCache = false;
                syncTicks--;

                if (syncTicks % 20 == 0) {
                    worldEntities.clear();

                    if (hasPlayers) {
                        for (World world : Bukkit.getWorlds()) {
                            worldEntities.put(world.getUID(), world.getEntities());
                        }
                    }
                }
            }

            if (hasPlayers) {
                for (SpartanPlayer p : players) {
                    if (clearCache) {
                        Cache.clearCheckCache(p);
                    }
                    p.cachedLocation = 0L;
                    p.nearbyEntities = null;
                    p.runCheck.clear();
                    p.runCheckAccountLag.clear();

                    // Separator
                    SpartanBukkit.runTask(p, () -> {
                        Player n = p.getPlayer();

                        if (n != null) {
                            p.setActivePotionEffects(n.getActivePotionEffects()); // Bad
                        }

                        // Ground Identification Cache
                        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                            p.trackGround();
                        }

                        // Preventions
                        HackPreventionUtils.run(p);
                    });
                }
            }
        } else if (players.size() > 0) {
            for (SpartanPlayer p : players) {
                SpartanBukkit.runTask(p, () -> {
                    if (p != null) {
                        Player n = p.getPlayer();

                        if (n != null) {
                            // Only
                            p.resetPing();
                            p.setFireTicks(n.getFireTicks());
                            p.setFrozen(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && n.isFrozen());
                            p.setInvulnerable(p.isFrozen() || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && n.isInvulnerable());

                            // Bad
                            p.setPlayerWeather(n.getPlayerWeather());
                            p.setHealth(n.getHealth());
                            p.setSleeping(n.isSleeping());
                            p.setInventory(n.getInventory(), n.getOpenInventory());

                            // Good
                            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                                ElytraUse.judge(p, n.isGliding(), false);
                            }
                            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                                p.setSwimming(n.isSwimming(), -1);
                            }
                            p.setGameMode(n.getGameMode());
                            p.setFlying(n.isFlying(), n.getAllowFlight());
                            p.setFoodLevel(n.getFoodLevel(), false);
                            p.setDead(n.isDead());
                            p.setWorld(n.getWorld());
                            p.setLastDamageCause(n.getLastDamageCause(), n.getLastDamage(), n.getMaximumNoDamageTicks());

                            // Ground Identification Cache
                            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                                p.trackGround();
                            }

                            // Buffer Cache
                            for (Buffer buffer : p.buffer) {
                                buffer.run(p);
                            }

                            // Check Allowance Cache
                            if (TPS.getTick(p) <= 100
                                    || !MaximumCheckedPlayers.isChecked(p.getUniqueId())
                                    || !SpartanEdition.hasDetectionsEnabled(p.dataType, false)
                                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && p.getGameMode() == GameMode.SPECTATOR
                                    || IDs.isPreview()
                                    || p.getCancellableCompatibility() != null) {
                                p.runChecks[0] = false;
                                p.runChecks[1] = false;
                            } else {
                                p.runChecks[0] = !LagLeniencies.hasInconsistencies(p, "");
                                p.runChecks[1] = true;
                            }

                            // External (Always Last & In Set Order)
                            PlayerData.update(p, n, MultiVersion.folia || !SpartanBukkit.isSynchronised());
                            MoveUtils.run(p);
                            PlayerData.run(p);
                            FromLocation.run(p);
                        } else {
                            p.runChecks[0] = false;
                            p.runChecks[1] = false;
                        }
                    }
                });
            }
        }
    }

    // Separator

    public SpartanPlayer(Player p) {
        UUID uuid = p.getUniqueId();
        PlayerInventory inv = p.getInventory();
        InventoryView openInventory = p.getOpenInventory();
        SpartanOpenInventory spartanOpenInv = new SpartanOpenInventory(
                openInventory.getCursor(),
                openInventory.countSlots(),
                openInventory.getTopInventory().getContents(),
                openInventory.getBottomInventory().getContents());
        Enums.HackType[] hackTypes = Enums.hackTypes();
        ItemStack itemInHand = p.getItemInHand();

        this.location = null;
        this.cachedLocation = 0L;
        this.lastLocation = 0L;
        this.nearbyEntities = null;
        this.nearbyEntitiesDistance = 0;
        this.runChecks = new boolean[]{false, false};
        this.runCheck = new LinkedHashMap<>(hackTypes.length);
        this.runCheckAccountLag = new LinkedHashMap<>(hackTypes.length);
        this.buffer = new Buffer[hackTypes.length + 1];
        this.timer = new Timer[hackTypes.length + 1];
        this.decimals = new Decimals[hackTypes.length + 1];
        this.tracker = new Tracker[hackTypes.length + 1];
        this.cooldowns = new Cooldowns[hackTypes.length + 1];

        for (Enums.HackType hackType : hackTypes) {
            int id = hackType.ordinal();
            boolean async = hackType.getCheck().canBeAsynchronous();
            this.buffer[id] = new Buffer(async);
            this.timer[id] = new Timer(async);
            this.decimals[id] = new Decimals(async);
            this.tracker[id] = new Tracker(async);
            this.cooldowns[id] = new Cooldowns(async);
        }
        this.buffer[hackTypes.length] = new Buffer(true);
        this.timer[hackTypes.length] = new Timer(true);
        this.decimals[hackTypes.length] = new Decimals(true);
        this.tracker[hackTypes.length] = new Tracker(true);
        this.cooldowns[hackTypes.length] = new Cooldowns(true);
        this.handlers = new Handlers();

        this.ipAddress = PlayerLimitPerIP.get(p);
        this.name = p.getName();
        this.uuid = uuid;
        this.flying = p.isFlying();
        this.allowFlight = p.getAllowFlight();
        this.dead = p.isDead();
        this.sleeping = p.isSleeping();
        this.walkSpeed = p.getWalkSpeed();
        this.sprinting = p.isSprinting();
        this.sneaking = p.isSneaking();
        this.eyeHeight = p.getEyeHeight();
        this.flySpeed = p.getFlySpeed();
        this.gameMode = p.getGameMode();
        this.health = p.getHealth();
        this.address = p.getAddress();
        this.usingItem = p.isBlocking();
        this.fireTicks = p.getFireTicks();
        this.lastDamage = p.getLastDamage();
        this.lastDamageCause = p.getLastDamageCause();
        this.openInventory = spartanOpenInv;
        this.maximumNoDamageTicks = p.getMaximumNoDamageTicks();
        this.fallDistance = p.getFallDistance();
        this.foodLevel = p.getFoodLevel();
        this.potionEffects = p.getActivePotionEffects().toArray(new PotionEffect[0]);
        this.playerWeather = p.getPlayerWeather();
        this.bedrockPlayer = BedrockCompatibility.isPlayer(p);
        this.dataType = bedrockPlayer ? ResearchEngine.DataType.Bedrock : ResearchEngine.DataType.Java;
        this.ping = Latency.ping(p);
        this.world = p.getWorld();
        this.frozen = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && p.isFrozen();
        this.lastPlayed = p.getLastPlayed();
        this.firstPlayed = p.getFirstPlayed();
        this.customDistance = 0.0;
        this.customHorizontalDistance = 0.0;
        this.customVerticalDistance = 0.0;
        this.nmsDistance = 0.0;
        this.nmsHorizontalDistance = 0.0;
        this.nmsVerticalDistance = 0.0;
        this.lastFall = 0L;
        this.lastOffSprint = 0L;
        this.lastJump = 0L;
        this.oldLastJump = 0L;
        this.airTicks = 0;
        this.oldAirTicks = 0;
        this.groundTicks = 0;
        this.extraPackets = 0;
        this.sprintingTime = 0L;
        this.jumpSprinting = 0L;
        this.walking = 0L;
        this.jumpWalking = 0;
        this.swimmingTime = 0L;
        this.flyingTime = 0L;
        this.chasing = 0L;
        this.lastLiquidTime = 0L;
        this.lastHeadMovement = System.currentTimeMillis();
        this.detectionTick = new DetectionTick();
        this.playerProfile = ResearchEngine.getPlayerProfile(this);
        this.clickData = new ClickData();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            this.gliding = p.isGliding();
            this.invulnerable = p.isInvulnerable();
            this.inventory = new SpartanInventory(inv.getContents(), new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}, itemInHand, inv.getItemInOffHand());

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                this.swimming = p.isSwimming();
            } else {
                this.swimming = false;
            }
        } else {
            this.gliding = false;
            this.invulnerable = false;
            this.inventory = new SpartanInventory(inv.getContents(), new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}, itemInHand, null);
            this.swimming = false;
        }
        this.creationTime = System.currentTimeMillis();
    }

    // Separator

    public Buffer[] getBuffers() {
        return buffer;
    }

    public Timer[] getTimers() {
        return timer;
    }

    public Decimals[] getDecimalss() {
        return decimals;
    }

    public Tracker[] getTrackers() {
        return tracker;
    }

    public Cooldowns[] getCooldownsList() {
        return cooldowns;
    }

    public Buffer getBuffer() {
        return buffer[Enums.hackTypeLength];
    }

    public Timer getTimer() {
        return timer[Enums.hackTypeLength];
    }

    public Decimals getDecimals() {
        return decimals[Enums.hackTypeLength];
    }

    public Tracker getTracker() {
        return tracker[Enums.hackTypeLength];
    }

    public Cooldowns getCooldowns() {
        return cooldowns[Enums.hackTypeLength];
    }

    public Buffer getBuffer(Enums.HackType hackType) {
        return buffer[hackType.ordinal()];
    }

    public Timer getTimer(Enums.HackType hackType) {
        return timer[hackType.ordinal()];
    }

    public Decimals getDecimals(Enums.HackType hackType) {
        return decimals[hackType.ordinal()];
    }

    public Tracker getTracker(Enums.HackType hackType) {
        return tracker[hackType.ordinal()];
    }

    public Cooldowns getCooldowns(Enums.HackType hackType) {
        return cooldowns[hackType.ordinal()];
    }

    public Handlers getHandlers() {
        return handlers;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean canSkipDetectionTick(int ticksCooldown) {
        return detectionTick.canSkip(uuid, playerProfile, cooldowns[Enums.hackTypeLength], ticksCooldown, this);
    }

    public int getCPS() {
        return clickData.getCount();
    }

    public ClickData getClickData() {
        return clickData;
    }

    public void calculateClickData(Action action, boolean runRegardless) {
        if (!runRegardless) {
            if (PlayerData.isInActivePlayerCombat(this)) {
                runRegardless = true;
            } else {
                SpartanBlock block = getTargetBlock(CombatUtils.maxHitDistance);
                runRegardless = block == null || BlockUtils.areAir(block.getType());
            }
        }

        if (runRegardless) {
            boolean success;

            if (action == Action.LEFT_CLICK_AIR) {
                success = true;
                clickData.calculate();
            } else if (action == null) {
                if (clickData.getLastCalculation() > 50L) {
                    success = true;
                    clickData.calculate();
                } else {
                    success = false;
                }
            } else {
                success = false;
            }

            if (success && clickData.canDistributeInformation()) {
                PlayerInfo.refresh(getName());
            }
        }
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public ResearchEngine.DataType getDataType() {
        return dataType;
    }

    private SpartanLocation getVehicleLocation() {
        Entity vehicle = getVehicle();

        if (vehicle instanceof LivingEntity || vehicle instanceof Vehicle) {
            Location vehicleLocation = vehicle.getLocation();
            SpartanLocation playerLocation = this.location;
            boolean isNull = playerLocation == null;
            return new SpartanLocation(this,
                    vehicleLocation,
                    isNull ? vehicleLocation.getYaw() : playerLocation.getYaw(),
                    isNull ? vehicleLocation.getPitch() : playerLocation.getPitch());
        }
        return null;
    }

    public void resetLocationData() {
        this.lastLocation = System.currentTimeMillis();
        this.cachedLocation = 0L;
    }

    public boolean canMaintainLocationData() {
        return (System.currentTimeMillis() - this.lastLocation) <= 1_000L && getVehicle() == null;
    }

    public void sendMessage(String message) {
        Player p = getPlayer();

        if (p != null) {
            p.sendMessage(message);
        }
    }

    public void sendInventoryCloseMessage(String message) {
        Player p = getPlayer();

        if (p != null) {
            if (message != null) {
                p.sendMessage(message);
            }
            p.closeInventory();
        }
    }

    public void sendImportantMessage(String message) {
        Player p = getPlayer();

        if (p != null) {
            p.sendMessage("");
            p.sendMessage(message);
            p.sendMessage("");
            p.closeInventory();
        }
    }

    public void openInventory(Inventory inventory) {
        if (inventory != null) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.openInventory(inventory);
            }
        }
    }

    public Inventory createInventory(int size, String title) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            return Bukkit.createInventory(p, size, title);
        }
        return null;
    }

    public SpartanLocation getLocation() {
        long ms = System.currentTimeMillis();

        if ((ms - this.cachedLocation) > 50L) {
            this.cachedLocation = ms;
            Player p = getPlayer();

            if (p != null) {
                SpartanLocation from = this.location;
                this.location = new SpartanLocation(this, p.getLocation());

                if (from != null && canMaintainLocationData()) {
                    this.location.retrieveDataFrom(from);
                } else {
                    this.lastLocation = System.currentTimeMillis();
                }
            }
        }
        SpartanLocation vehicle = getVehicleLocation();
        return vehicle != null ? vehicle : location == null ? new SpartanLocation() : location.clone();
    }

    public SpartanLocation getEventLocation(Location to) {
        SpartanLocation vehicle = getVehicleLocation();
        return vehicle != null ? vehicle : new SpartanLocation(this, to);
    }

    public boolean debug(boolean broadcast, boolean cutDecimals, Object... message) {
        if (!Piracy.enabled && !IDs.isBuiltByBit() && !IDs.isPolymart() && !IDs.isSongoda() && Bukkit.getMotd().contains(Register.plugin.getName())) {
            Player p = getPlayer();

            if (p != null && p.isWhitelisted()) {
                String string = ChatColor.GRAY + p.getName() + ChatColor.DARK_GRAY + ": " + ChatColor.RED;

                if (message == null || message.length == 0) {
                    string += new Random().nextInt();
                } else {
                    if (cutDecimals) {
                        int i = 0;

                        for (Object object : message) {
                            if (object instanceof Double) {
                                message[i] = AlgebraUtils.cut((double) object, 5);
                            }
                            i++;
                        }
                    }
                    string += Arrays.toString(message);
                }
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (players.size() > 0) {
                    if (broadcast) {
                        Bukkit.broadcastMessage(string);
                    } else {
                        for (SpartanPlayer o : players) {
                            if (o != null && o.isOp()) {
                                Player no = o.getPlayer();

                                if (no != null) {
                                    no.sendMessage(string);
                                }
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

    public boolean canRunChecks(boolean accountLag) {
        return runChecks[accountLag ? 0 : 1];
    }

    public boolean canRunCheck(Enums.HackType hackType, boolean accountLag) {
        if (canRunChecks(accountLag)) {
            Map<Enums.HackType, Boolean> map = accountLag ? runCheckAccountLag : runCheck;
            Boolean bool = map.get(hackType);

            if (bool != null) {
                return bool;
            }
            bool = SpartanEdition.supportsCheck(dataType, hackType)
                    && hackType.getCheck().isEnabled(this.getWorld().getName(), this.getUniqueId())
                    && !Permissions.isBypassing(this, hackType);
            map.put(hackType, bool);
            return bool;
        }
        return false;
    }

    public int getFireTicks() {
        return fireTicks;
    }

    public boolean isOnFire() {
        return getFireTicks() > -20 && !hasPotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }

    public void setFireTicks(int fireTicks) {
        this.fireTicks = fireTicks;
    }

    public boolean isOp() {
        Player p = this.getPlayer();
        return p != null && p.isOp();
    }

    public boolean isWhitelisted() {
        Player p = this.getPlayer();
        return p != null && p.isWhitelisted();
    }

    public String getCancellableCompatibility() {
        return MythicMobs.is(this) ? Compatibility.CompatibilityType.MythicMobs.toString() :
                ItemsAdder.is(this) ? Compatibility.CompatibilityType.ItemsAdder.toString() :
                        CustomEnchantsPlus.has(this) ? Compatibility.CompatibilityType.CustomEnchantsPlus.toString() :
                                EcoEnchants.has(this) ? Compatibility.CompatibilityType.EcoEnchants.toString() : null;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public WeatherType getPlayerWeather() {
        return playerWeather;
    }

    public void setPlayerWeather(WeatherType weatherType) {
        playerWeather = weatherType;
    }

    public boolean isFlying() {
        Entity vehicle = getVehicle();
        return vehicle != null ? vehicle instanceof Player && ((Player) vehicle).isFlying() : flying;
    }

    public boolean getAllowFlight() {
        return allowFlight;
    }

    public boolean wasFlying() {
        return isFlying() || flyingTime >= System.currentTimeMillis();
    }

    public long getLastLiquidTime() {
        return lastLiquidTime;
    }

    public boolean wasInLiquids() {
        return System.currentTimeMillis() - lastLiquidTime <= 750L;
    }

    public void setLastLiquidTime() {
        lastLiquidTime = System.currentTimeMillis();
    }

    public void setFlying(boolean flying, boolean allowFlight) {
        if (!flying) {
            flying = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && gameMode == GameMode.SPECTATOR;
        }
        this.flying = flying;
        this.allowFlight = allowFlight;

        if (flying) {
            if (gameMode == GameMode.CREATIVE) {
                handlers.add(Handlers.HandlerType.GameMode, 20);
            }
            this.flyingTime = System.currentTimeMillis() + 3_000L;
        }
    }

    public boolean hasShield() {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && !Compatibility.CompatibilityType.OldCombatMechanics.isFunctional()
                && (PlayerData.hasItemInHands(this, Material.SHIELD)
                || isUsingItem());
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public boolean isSleeping() {
        return sleeping;
    }

    public void setSleeping(boolean sleeping) {
        this.sleeping = sleeping;
    }

    public void setLastHeadMovement() {
        this.lastHeadMovement = System.currentTimeMillis();
    }

    public long getLastHeadMovement() {
        return System.currentTimeMillis() - lastHeadMovement;
    }

    public boolean isMoving(boolean head) {
        return (!head || getLastHeadMovement() <= 2_500L)
                && (getNmsDistance() >= 0.1
                || getCustomDistance() >= 0.1
                || isSprinting() || isWalking()
                || isSprintJumping() || isWalkJumping()
                || Damage.getLastReceived(this) <= 100L);
    }

    public boolean isSprinting() {
        return sprinting || isCustomSprinting();
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isOnGroundCustom() {
        return onGroundCustom;
    }

    public boolean isOnGround() {
        Player p = this.getPlayer();
        return p != null && p.isOnGround();
    }

    private void trackGround() {
        if (!TestServer.isIdentified() && this.isOnGround()

                && (Settings.getBoolean("Performance.use_vanilla_ground_method")
                || Compatibility.CompatibilityType.MajorIncompatibility.isFunctional()
                || LagLeniencies.hasInconsistencies(this, "tps"))) {
            this.onGroundCustom = true; // Vanilla On-Ground When Necessary
        } else if (PlayerData.isOnGround(this, this.getLocation(), 0.0)) {
            this.onGroundCustom = true; // Server Current Location
        } else {
            SpartanLocation from = MoveUtils.getCachedLocation(this, true);

            if (from != null && PlayerData.isOnGround(this, from, 0.0)) {
                this.onGroundCustom = true; // Server Previous Location
            } else {
                PlayerMoveEvent event = EventsHandler7.getMovementEvent(this);

                if (event != null) {
                    Location eventFrom = event.getFrom();
                    Runnable runnable = () -> {
                        if (PlayerData.isOnGround(this, new SpartanLocation(this, eventFrom), 0.0)) {
                            this.onGroundCustom = true; // Event Previous Location
                        } else {
                            Location eventLocation = event.getTo();
                            this.onGroundCustom = eventLocation != null && PlayerData.isOnGround(this, new SpartanLocation(this, eventLocation), 0.0); // Event Current Location
                        }
                    };

                    if (MultiVersion.folia) {
                        SpartanBukkit.runTask(
                                this,
                                eventFrom.getWorld(),
                                eventFrom.getBlockX() >> 4,
                                eventFrom.getBlockZ() >> 4,
                                runnable
                        );
                    } else {
                        runnable.run();;
                    }
                } else {
                    this.onGroundCustom = false;
                }
            }
        }
    }

    public void removePotionEffect(PotionEffectType potionEffectType) {
        if (hasPotionEffect(potionEffectType)) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.removePotionEffect(potionEffectType);
                setActivePotionEffects(p.getActivePotionEffects());
            }
        }
    }

    public void addPotionEffect(PotionEffect potionEffect) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            p.addPotionEffect(potionEffect);
            setActivePotionEffects(p.getActivePotionEffects());
        }
    }

    public boolean canSee(SpartanPlayer target) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            Player t = target.getPlayer();

            if (t != null && t.isOnline()) {
                return p.canSee(t);
            }
        }
        return true;
    }

    public void showPlayer(SpartanPlayer target) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            Player t = target.getPlayer();

            if (t != null && t.isOnline()) {
                p.showPlayer(t);
            }
        }
    }

    public void hidePlayer(SpartanPlayer target) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            Player t = target.getPlayer();

            if (t != null && t.isOnline()) {
                p.hidePlayer(t);
            }
        }
    }

    public void kickPlayer(String reason) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            p.kickPlayer(reason);
        }
    }

    public void damage(double amount, EntityDamageEvent.DamageCause damageCause) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            EntityDamageEvent event = new EntityDamageEvent(p, damageCause, amount);
            p.damage(amount);
            p.setLastDamageCause(event);
            setLastDamageCause(event, amount, p.getMaximumNoDamageTicks());
        }
    }

    public PlayerProfile getProfile() {
        return playerProfile;
    }

    public void setProfile(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public SpartanBlock getTargetBlock(double distance) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            try {
                Player n = getPlayer();

                if (n != null) {
                    Block block = n.getTargetBlock(null, (int) distance);

                    if (block != null) {
                        return new SpartanBlock(this, block);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public Entity getVehicle() {
        Player p = this.getPlayer();
        return p == null || !p.isOnline() ? null : p.getVehicle();
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public void setWalkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    public void setFlySpeed(float flySpeed) {
        this.flySpeed = flySpeed;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int foodLevel, boolean modify) {
        if (modify) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.setFoodLevel(foodLevel);
                this.foodLevel = foodLevel;
            }
        } else {
            this.foodLevel = foodLevel;
        }
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public float getFallDistance() {
        return fallDistance;
    }

    public void setFallDistance(float fallDistance, boolean modify) {
        if (modify) {
            Player p = getPlayer();

            if (p != null && p.isOnline()) {
                p.setFallDistance(fallDistance);
                this.fallDistance = fallDistance;
            }
        } else {
            this.fallDistance = fallDistance;
        }
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public double getEyeHeight() {
        return eyeHeight;
    }

    public void setEyeHeight(double eyeHeight) {
        this.eyeHeight = eyeHeight;
    }

    public boolean isBedrockPlayer() {
        return bedrockPlayer;
    }

    public boolean isGliding() {
        return gliding;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public void setGliding(boolean gliding, boolean modify) {
        this.gliding = gliding;

        if (modify && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            Player p = getPlayer();

            if (p != null) {
                p.setGliding(gliding);
            }
        }
    }

    public boolean isSwimming() {
        return swimming || swimmingTime >= System.currentTimeMillis();
    }

    public void setSwimming(boolean swimming, int ticks) {
        this.swimming = swimming;

        if (ticks != -1L) {
            this.swimmingTime = (ticks > 0 ? System.currentTimeMillis() + (ticks * 50L) : 0L);
        }
    }

    public ItemStack getItemInHand() {
        return inventory.getItemInHand();
    }

    public SpartanOpenInventory getOpenInventory() {
        return openInventory;
    }

    private void prepareForAbstractMovement() {
        setSprinting(false);
        setSneaking(false);
        setSwimming(false, 0);
        setGliding(false, false);
    }

    public boolean teleport(SpartanLocation location) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            p.leaveVehicle();
            prepareForAbstractMovement();

            if (MultiVersion.folia) {
                p.teleportAsync(location.getBukkitLocation());
            } else {
                p.teleport(location.getBukkitLocation());
            }
            return true;
        }
        return false;
    }

    public void setVelocity(Vector vector) {
        Player p = getPlayer();

        if (p != null && p.isOnline()) {
            p.leaveVehicle();
            prepareForAbstractMovement();
            p.setVelocity(vector);
        }
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public long getFirstPlayed() {
        return firstPlayed;
    }

    public boolean isUsingItem() {
        return usingItem;
    }

    public void setUsingItem(boolean usingItem) {
        this.usingItem = usingItem;
    }

    public void resetPing() {
        ping = -1;
    }

    public int getPing() {
        if (ping == -1) {
            Player p = this.getPlayer();
            return p != null ? (ping = Latency.ping(p)) : 0;
        }
        return ping;
    }

    public double getLastDamage() {
        return lastDamage;
    }

    public EntityDamageEvent getLastDamageCause() {
        return lastDamageCause == null ? new EntityDamageEvent(getPlayer(), EntityDamageEvent.DamageCause.CUSTOM, 0) : lastDamageCause;
    }

    public void setLastDamageCause(EntityDamageEvent lastDamageCause, double lastDamage, int maximumNoDamageTicks) {
        this.lastDamageCause = lastDamageCause;
        this.lastDamage = lastDamage;
        this.maximumNoDamageTicks = maximumNoDamageTicks;
    }

    public int getMaximumNoDamageTicks() {
        return maximumNoDamageTicks;
    }

    public boolean hasAttackCooldown() {
        return getAttackCooldown() != 1.0f;
    }

    public float getAttackCooldown() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            Player player = getPlayer();
            return player == null ? 1.0f : player.getAttackCooldown();
        }
        return 1.0f;
    }

    public PotionEffect[] getActivePotionEffects() {
        return potionEffects;
    }

    public void setActivePotionEffects(Collection<PotionEffect> potionEffects) {
        this.potionEffects = potionEffects.toArray(new PotionEffect[0]);
    }

    public PotionEffect getPotionEffect(PotionEffectType type) {
        if (potionEffects.length > 0) {
            for (PotionEffect potionEffect : potionEffects) {
                if (potionEffect.getType().equals(type)) {
                    return potionEffect;
                }
            }
        }
        return null;
    }

    public boolean hasPotionEffect(PotionEffectType[] potionEffectTypes) {
        if (potionEffects.length > 0) {
            for (PotionEffect potionEffect : potionEffects) {
                for (PotionEffectType potionEffectType : potionEffectTypes) {
                    if (potionEffect.getType().equals(potionEffectType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean hasPotionEffect(PotionEffectType potionEffectType) {
        if (potionEffects.length > 0) {
            for (PotionEffect potionEffect : potionEffects) {
                if (potionEffect.getType().equals(potionEffectType)) {
                    return true;
                }
            }
        }
        return false;
    }


    public boolean canSee(Player target) {
        if (SpartanBukkit.supportedFork) {
            Player player = getPlayer();

            if (player != null) {
                return player.canSee(target);
            }
        }
        return true;
    }

    public SpartanInventory getInventory() {
        return inventory;
    }

    public void setInventory(PlayerInventory inv, InventoryView openInventory) {
        this.inventory = new SpartanInventory(inv.getContents(),
                new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()},
                inv.getItemInHand(), MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) ? inv.getItemInOffHand() : null);

        if (openInventory != null) {
            this.openInventory = new SpartanOpenInventory(openInventory.getCursor(),
                    openInventory.countSlots(),
                    openInventory.getTopInventory().getContents(),
                    openInventory.getBottomInventory().getContents());
        } else {
            this.openInventory = new SpartanOpenInventory();
        }
    }

    public List<Entity> getNearbyEntities(double x, double y, double z, int limit) {
        double d = Math.max(Math.max(x, y), z);
        List<Entity> nearbyEntitiesThreadSafe;

        // Caching
        if (nearbyEntities == null || d > nearbyEntitiesDistance) {
            Player p = this.getPlayer();

            if (p != null) {
                nearbyEntitiesDistance = d;

                if (SpartanBukkit.isSynchronised()) {
                    nearbyEntities = (nearbyEntitiesThreadSafe = new CopyOnWriteArrayList<>(p.getNearbyEntities(x, y, z)));
                } else {
                    SpartanLocation loc = this.getLocation();
                    List<Entity> entities = worldEntities.get(loc.getWorld().getUID());

                    if (entities != null && entities.size() > 0) {
                        nearbyEntitiesThreadSafe = new CopyOnWriteArrayList<>();
                        entities.remove(p);

                        for (Entity entity : entities) {
                            if ((entity instanceof Vehicle
                                    || !(entity instanceof LivingEntity)
                                    || !entity.isDead())

                                    && loc.distance(entity.getLocation()) <= d) {
                                nearbyEntitiesThreadSafe.add(entity);
                            }
                        }
                        return nearbyEntities = nearbyEntitiesThreadSafe;
                    } else {
                        return nearbyEntities = new ArrayList<>(0);
                    }
                }
            } else {
                return new ArrayList<>(0);
            }
        } else {
            nearbyEntitiesThreadSafe = nearbyEntities;
        }

        // Retrieving
        int size = nearbyEntitiesThreadSafe.size();

        if (size > 0) {
            int counter = 0;
            List<Entity> result = new ArrayList<>(size);
            SpartanLocation loc = this.getLocation();
            nearbyEntitiesThreadSafe.remove(getVehicle());

            for (Entity entity : nearbyEntitiesThreadSafe) {
                if (loc.distance(entity.getLocation()) <= d) {
                    result.add(entity);
                    counter++;

                    if (counter == limit) {
                        break;
                    }
                }
            }
            return result;
        }
        return nearbyEntitiesThreadSafe;
    }

    public List<Entity> getNearbyEntities(double x, double y, double z) {
        return getNearbyEntities(x, y, z, 0);
    }

    public boolean hasNearbyEntities(double x, double y, double z) {
        return getNearbyEntities(x, y, z, 1).size() > 0;
    }

    // Custom

    // Teleport

    public boolean safeTeleport(SpartanLocation loc) {
        if (!isSleeping()) {
            double x = Math.abs(loc.getX()),
                    y = Math.abs(loc.getY()),
                    z = Math.abs(loc.getZ());

            if (AlgebraUtils.hasExponent(x) || AlgebraUtils.hasExponent(y) || AlgebraUtils.hasExponent(z)
                    || x > 3.0E7 || y == Double.MAX_VALUE || z > 3.0E7 || loc.getBlockY() == Integer.MAX_VALUE) {
                return false;
            }
            GroundUtils.clear(this); // Attention

            if (getWorld() == loc.getWorld()) {
                SpartanLocation to = getLocation();
                return teleport(new SpartanLocation(this, loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), to.getYaw(), to.getPitch()));
            }
            return teleport(getLocation());
        }
        return false;
    }

    public boolean groundTeleport(boolean checkGround) {
        boolean testServer = TestServer.isIdentified();

        if (!testServer && !Settings.getBoolean("Detections.ground_teleport_on_detection")) {
            return false;
        }
        SpartanLocation ploc = getLocation();

        if (checkGround && GroundUtils.isOnGround(this, ploc, 0.0, false, false)) {
            return false;
        }
        SpartanLocation ploc1 = ploc.clone().add(0, 1, 0);

        if (BlockUtils.isSolid(ploc1) && !(BlockUtils.areWalls(ploc1) || BlockUtils.canClimb(ploc1))) {
            return false;
        }
        GroundUtils.clear(this); // Attention
        double y = ploc.getY();
        double counted_damage = 0.0;
        double standingBlockHeight = GroundUtils.getHeight(ploc.getBlock().getType());

        for (double i = -2.0; i <= y; i++) {
            counted_damage += 1.0;
            SpartanLocation loc = getLocation();
            loc.setY(Math.floor(y - i) + standingBlockHeight); // Attention
            Material material = loc.getBlock().getType();
            int blockY = loc.getBlockY();

            if ((BlockUtils.canClimb(material) || BlockUtils.areWalls(material) || !BlockUtils.isSolid(material) || blockY <= 0) && blockY > MoveUtils.voidDamageHeight) {
                safeTeleport(loc);
            } else {
                if (getProfile().isSuspectedOrHacker() && blockY > BlockUtils.getMinHeight(world)) {
                    boolean is_bigger = counted_damage >= 4.0 && counted_damage >= getFallDistance();
                    boolean can_damage = testServer || Settings.getBoolean("Detections.fall_damage_on_teleport");

                    if (is_bigger || !can_damage) {
                        setFallDistance(0.0f, true);
                    }
                    if (is_bigger && can_damage) {
                        applyFallDamage(counted_damage);
                    }
                }
                break;
            }
        }
        return true;
    }

    // Damage

    public void applyFallDamage(double d) {
        handlers.disable(Handlers.HandlerType.Floor, Teleport.ticks);
        handlers.disable(Handlers.HandlerType.Velocity, Teleport.ticks);
        handlers.disable(Handlers.HandlerType.Teleport, 1);
        damage(d, EntityDamageEvent.DamageCause.FALL);
    }

    // Ground

    public int getBlocksOffGround(int limit, boolean liquid, boolean climbable) {
        boolean hasLimit = limit > 0;
        SpartanLocation ploc = getLocation();
        Decimals decimals = getDecimals();
        Cooldowns cooldowns = getCooldowns();
        String key = "blocks-off-ground=" + Objects.hash(ploc.getBlockY(), hasLimit ? limit : 0, liquid, climbable);

        if (!cooldowns.canDo(key)) {
            return (int) decimals.get(key, 0.0);
        }
        if (GroundUtils.isOnGround(this, ploc, 0.0, liquid, climbable)
                || BlockUtils.isSolid(ploc.clone().add(0, 1, 0))) {
            return 0;
        }
        int blocksOffGround = 0;

        for (double i = 0.0; i <= ploc.getY(); i++) {
            SpartanBlock block = ploc.clone().add(0, -i, 0).getBlock();

            if (!block.isLiquid()) {
                Material material = block.getType();

                if (!BlockUtils.isSolid(material)) {
                    blocksOffGround++;

                    if (hasLimit && blocksOffGround == limit) {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        decimals.set(key, blocksOffGround);
        cooldowns.add(key, 4);
        return blocksOffGround;
    }

    public int getBlocksOffGround(boolean liquid, boolean climbable) {
        return getBlocksOffGround(0, liquid, climbable);
    }

    public boolean isGroundSpoofing() {
        return isOnGroundCustom() != isOnGround();
    }

    // Jumping

    public boolean isJumping(double d) {
        return MoveUtils.isJumping(d, isBedrockPlayer() ? MoveUtils.lowPrecision : MoveUtils.jumping[2])

                || hasPotionEffect(PotionEffectType.JUMP)
                && MoveUtils.isJumping(d - getExtraMovementFromJumpEffect(), MoveUtils.lowPrecision);
    }

    public long getLastJump() {
        return System.currentTimeMillis() - lastJump;
    }

    public void setLastJump() {
        this.lastJump = System.currentTimeMillis();
    }

    public long getOldLastJump() {
        return System.currentTimeMillis() - oldLastJump;
    }

    public void setOldLastJump() {
        this.oldLastJump = lastJump;
    }

    public boolean isFalling(double dy) {
        if (dy < 0.0) {
            for (double divisor : MoveUtils.falling) {
                double division = dy / divisor;
                division -= Math.floor(division);

                if (division < 0.1 || division > 0.9) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isClimbing(double d) {
        double v5 = AlgebraUtils.cut(d, 5);

        if (v5 != MoveUtils.falling[0] && v5 != MoveUtils.falling[1]) {
            for (double value : MoveUtils.climbing) {
                if (Math.abs(value - Math.abs(d)) < MoveUtils.lowPrecision) {
                    return true;
                }
            }
        }
        return false;
    }

    public double getExtraMovementFromJumpEffect() {
        return PlayerData.getPotionLevel(this, PotionEffectType.JUMP) * 0.1;
    }

    public double getJumpEffectLimit(double v) {
        double additional = getExtraMovementFromJumpEffect();

        if (additional > 0.0) {
            v += (additional + MoveUtils.highPrecision);
        }
        return v;
    }

    // Allowance

    public boolean canDo(boolean bypassItemAttributes) {
        if ((bypassItemAttributes || !Attributes.has(this, Attributes.GENERIC_MOVEMENT_SPEED))
                && !isDead() && !isSleeping() && canRunChecks(true) && !wasFlying() && !Velocity.hasCooldown(this)) {
            if (Compatibility.CompatibilityType.MythicMobs.isFunctional() || Compatibility.CompatibilityType.ItemsAdder.isFunctional()) {
                List<Entity> entities = getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

                if (entities.size() > 0) {
                    for (Entity entity : entities) {
                        if (MythicMobs.is(entity) || ItemsAdder.is(entity)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean canDoAccurately() {
        return !isDead() && !isFlying() && !isSleeping()
                && canRunChecks(false)
                && !Velocity.hasCooldown(this);
    }

    // Chasing

    public boolean isChasing() {
        if (chasing >= System.currentTimeMillis()) {
            return true;
        }
        if (!playerProfile.isHacker()
                && isMoving(false) // Make sure the chaser is moving
                && CombatProcessing.getDecimal(this, CombatProcessing.yawDifference, 0.0) <= 45.0
                && CombatProcessing.getDecimal(this, CombatProcessing.pitchDifference, 0.0) <= 22.5) {
            List<Entity> entities = getNearbyEntities(CombatUtils.maxHitDistance, CombatUtils.maxHitDistance, CombatUtils.maxHitDistance);

            if (entities.size() > 0) { // Make sure there are entities nearby in a hit distance
                for (Entity entity : entities) {
                    if (entity instanceof Player) { // Make sure the entity is a player
                        SpartanPlayer target = SpartanBukkit.getPlayer((Player) entity);

                        if (target != null && target.isMoving(false)) { // Make sure the target is also moving
                            PlayerFight fight = getProfile().getCombat().getCurrentFight(target);
                            PlayerOpponent[] playerOpponents = fight.getOpponent(this);

                            if (playerOpponents[0].getLastHit() <= PlayerData.combatTimeRequirement // Make sure the player has recently hit the target
                                    && (playerOpponents[0].hasHitCombo() || !CombatUtils.areFacingEachOther(this, (LivingEntity) entity)) // Make sure the player has a hit combo or is facing a different direction
                                    && playerOpponents[1].getLastDamage() <= PlayerData.combatTimeRequirement) {  // Make sure the target has recently received damage by the player
                                double[] utility = CombatUtils.get_X_Y_Distance(this, (LivingEntity) entity);

                                if (utility != null) {
                                    double distance = utility[2];

                                    if (distance >= CombatUtils.maxLegitimateHitDistance) { // Make sure the distance of the two players is beyond legitimacy
                                        chasing = System.currentTimeMillis() + PlayerData.combatTimeRequirement;
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // Environment

    public boolean stepsOnBoats() {
        return GroundUtils.stepsOnBoats(this, getCooldowns(), getLocation());
    }

    // Falling

    public double getCalculatedFallingRatio(double vertical) {
        if (vertical < 0.0) {
            vertical -= MoveUtils.falling[0];
            double calculated = MoveUtils.falling[0] * (getTicksOnAir() + 1);
            return Math.abs(calculated - Math.abs(vertical));
        }
        return 0.0;
    }

    public long getLastFall() {
        return System.currentTimeMillis() - lastFall;
    }

    public void setLastFall() {
        this.lastFall = System.currentTimeMillis();
    }

    // Base

    public void resetHandlers() {
        for (Handlers.HandlerType handlerType : Handlers.HandlerType.values()) {
            handlers.remove(handlerType);
        }
    }

    // Speed

    public int getSneakingCount() {
        return getBuffer().get("sneaking-counter");
    }

    public long getLastOffSprint() {
        return System.currentTimeMillis() - lastOffSprint;
    }

    public void setLastOffSprint() {
        this.lastOffSprint = System.currentTimeMillis();
    }

    // Distance

    public double getCustomDistance() {
        return customDistance;
    }

    public void setCustomDistance(double distance) {
        this.customDistance = distance;
    }

    public double getCustomHorizontalDistance() {
        return customHorizontalDistance;
    }

    public void setCustomHorizontalDistance(double distance) {
        this.customHorizontalDistance = distance;
    }

    public double getCustomVerticalDistance() {
        return customVerticalDistance;
    }

    public void setCustomVerticalDistance(double distance) {
        this.customVerticalDistance = distance;
    }

    public double getNmsDistance() {
        return getCustomDistance() == 0.0 ? 0.0 : nmsDistance;
    }

    public void setNmsDistance(double distance) {
        this.nmsDistance = distance;
    }

    public double getNmsHorizontalDistance() {
        return getCustomDistance() == 0.0 ? 0.0 : nmsHorizontalDistance;
    }

    public void setNmsHorizontalDistance(double distance) {
        this.nmsHorizontalDistance = distance;
    }

    public double getNmsVerticalDistance() {
        return getCustomDistance() == 0.0 ? 0.0 : nmsVerticalDistance;
    }

    public void setNmsVerticalDistance(double distance) {
        this.nmsVerticalDistance = distance;
    }

    // Direction

    public SpartanBlock getIllegalTargetBlock(SpartanBlock clickedBlock) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            SpartanLocation clickedBlockLocation = clickedBlock.getLocation();
            boolean editableClickedBlock = BlockUtils.isChangeable(clickedBlockLocation);

            if (!editableClickedBlock && !BlockUtils.isSolid(clickedBlockLocation)) {
                return null;
            }
            double distance = getLocation().distance(clickedBlockLocation);

            if (distance >= 0.5) {
                try {
                    SpartanBlock targetBlock = getTargetBlock(Math.max(Math.floor(distance), 1.0));

                    if (targetBlock != null && BlockUtils.isSolid(targetBlock.getType())) {
                        SpartanLocation targetBlockLocation = targetBlock.getLocation();

                        if (clickedBlockLocation.distance(targetBlockLocation) >= (handlers.has(Handlers.HandlerType.ElytraUse) ? 2.5 : BlockUtils.areEggs(getItemInHand().getType()) ? 2.0 : 1.0)

                                && (targetBlock.getX() != clickedBlock.getX()
                                || targetBlock.getY() != clickedBlock.getY()
                                || targetBlock.getZ() != clickedBlock.getZ())

                                && (editableClickedBlock && !BlockUtils.isLiquid(targetBlockLocation)
                                || BlockUtils.isFullSolid(targetBlockLocation, true))) {
                            return targetBlock;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    // Border

    public boolean isOutsideOfTheBorder() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && !ViaVersion.isUsing(this, ViaVersion.v1_7)) {
            SpartanLocation loc = getLocation();
            WorldBorder border = getWorld().getWorldBorder();
            double size = border.getSize() / 2.0;
            Location center = border.getCenter();
            return Math.abs(loc.getX() - center.getX()) > size
                    || Math.abs(loc.getZ() - center.getZ()) > size;
        }
        return false;
    }

    public boolean isInTheBorder() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && !ViaVersion.isUsing(this, ViaVersion.v1_7)) {
            SpartanLocation loc = getLocation();
            WorldBorder border = getWorld().getWorldBorder();
            double sizeMinus = (border.getSize() / 2.0) - 1.0,
                    sizePlus = sizeMinus + 2.0;
            Location center = border.getCenter();
            double x = Math.abs(loc.getX() - center.getX()),
                    z = Math.abs(loc.getZ() - center.getZ());
            return x >= sizeMinus && x <= sizePlus
                    || z >= sizeMinus && z <= sizePlus;
        }
        return false;
    }

    // Counters

    public int getExtraPackets() {
        return isUsingItem() ? 0 : extraPackets;
    }

    public void setExtraPackets(int number) {
        this.extraPackets = number;
    }

    public int getTicksOnAir() {
        return airTicks;
    }

    public void setAirTicks(int number) {
        if (airTicks > 0) {
            setOldAirTicks(airTicks);
        }
        this.airTicks = number;
    }

    public int getOldTicksOnAir() {
        return oldAirTicks;
    }

    public void setOldAirTicks(int number) {
        this.oldAirTicks = number;
    }

    public int getTicksOnGround() {
        return groundTicks;
    }

    public void setGroundTicks(int number) {
        this.groundTicks = number;
    }

    // Movement

    public boolean isCustomSprinting() {
        return sprintingTime > System.currentTimeMillis();
    }

    public void setSprinting(int ticks) {
        this.sprintingTime = System.currentTimeMillis() + (ticks * 50L);
    }

    public boolean isWalking() {
        return walking > System.currentTimeMillis();
    }

    public void setWalking(int ticks) {
        this.walking = System.currentTimeMillis() + (ticks * 50L);
    }

    public boolean isWalkJumping() {
        return jumpWalking > System.currentTimeMillis();
    }

    public void setJumpWalking(int ticks) {
        this.jumpWalking = System.currentTimeMillis() + (ticks * 50L);
    }

    public boolean isSprintJumping() {
        return jumpSprinting > System.currentTimeMillis();
    }

    public void setJumpSprinting(int ticks) {
        this.jumpSprinting = System.currentTimeMillis() + (ticks * 50L);
    }

    public boolean isCrawling() {
        return getEyeHeight() <= 1.2 && !isGliding() && !isSwimming();
    }
}
