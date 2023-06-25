package me.vagdedes.spartan.objects.replicates;

import me.vagdedes.spartan.Register;
import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.handlers.stability.Chunks;
import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.system.Check;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.CombatUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;
import me.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpartanLocation {

    private static class BlockCache {

        private final long time;
        private final SpartanBlock block;

        public BlockCache(SpartanBlock block) {
            this.block = block;
            this.time = System.currentTimeMillis();
        }
    }

    static final Map<Integer, BlockCache> memory = new ConcurrentHashMap<>();
    public static final int clearanceTick = 4;
    private static final long clearanceTime = clearanceTick * TPS.tickTime;
    private static final boolean
            v_1_9 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9),
            v_1_13 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13),
            v_1_17 = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17);

    static {
        if (Register.isPluginLoaded()) {
            // Synchronised due to most calls being on the main thread
            SpartanBukkit.runRepeatingTask(() -> {
                if (memory.size() > 0) {
                    long time = System.currentTimeMillis();
                    Iterator<BlockCache> iterator = memory.values().iterator();

                    while (iterator.hasNext()) {
                        BlockCache cache = iterator.next();

                        if ((time - cache.time) >= clearanceTime) {
                            iterator.remove();
                        }
                    }
                }
            }, 1L, clearanceTick);
        }
    }

    public static double distance(Location loc1, Location loc2) {
        return loc1.getWorld() != loc2.getWorld() ? 0.0 :
                AlgebraUtils.getDistance(loc1.getX(), loc2.getX(), loc1.getY(), loc2.getY(), loc1.getZ(), loc2.getZ());
    }

    // Object

    private final SpartanPlayer player;
    private final World serverWorld;
    private Chunk chunk;
    private double x, y, z;
    private float yaw, pitch;
    private Vector vector;
    private SpartanBlock block;

    // Utilities

    private void loadCoordinates(Chunk chunk, double x, double y, double z, float yaw, float pitch) {
        this.chunk = chunk;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.vector = null;
        this.block = null;
    }

    private SpartanBlock loadBlock(Material material, byte data, boolean liquid, boolean waterLogged) {
        return this.block = new SpartanBlock(this.player, this.serverWorld, this.chunk, material, data, getBlockX(), getBlockY(), getBlockZ(), liquid, waterLogged);
    }

    private void loadEmptyBlock(boolean air) {
        loadBlock(air ? Material.AIR : Material.STONE, (byte) 0, false, false);
    }

    // Base

    private SpartanLocation(SpartanPlayer player, World world) { // Used for inititation
        this.player = player;
        this.serverWorld = world;
    }

    SpartanLocation() { // Used as a backup
        this(null, Bukkit.getWorlds().get(0));
        loadCoordinates(null, 0, 0, 0, 0, 0);
        loadEmptyBlock(false);
    }

    SpartanLocation(SpartanPlayer player, World world, Chunk chunk, double x, double y, double z, float yaw, float pitch, SpartanBlock block) { // Used for objects
        this(player, world);
        loadCoordinates(chunk, x, y, z, yaw, pitch);

        if (block != null) {
            loadBlock(block.getType(), block.getData(), block.isLiquid(), block.isWaterLogged());
        } else {
            this.block = null;
        }
    }

    // Native

    SpartanLocation(SpartanPlayer player, Location loc, float yaw, float pitch) { // Used for vehicles
        this(player, loc.getWorld());
        loadCoordinates(null, loc.getX(), loc.getY(), loc.getZ(), yaw, pitch);

        if (!v_1_9) { // 1.8 or older
            if (SpartanBukkit.isSynchronised()) {
                Block block = loc.getBlock();
                loadBlock(block.getType(), block.getData(), BlockUtils.isLiquid(block), false);
            } else {
                loadEmptyBlock(false);
            }
        }
    }

    public SpartanLocation(SpartanPlayer player, World world, double x, double y, double z, float yaw, float pitch) { // Used globally
        this(player, world, null, x, y, z, yaw, pitch, null);
    }

    public SpartanLocation(SpartanPlayer player, Location loc) { // Used globally
        this(player, loc, loc.getYaw(), loc.getPitch());
    }

    private SpartanLocation(SpartanLocation loc) { // Used for copying
        this(loc.player, loc.serverWorld);
        loadCoordinates(loc.chunk, loc.x, loc.y, loc.z, loc.yaw, loc.pitch);
        this.block = loc.block;
        this.vector = loc.vector;
    }

    // Methods

    public SpartanLocation clone() {
        return new SpartanLocation(this);
    }

    public SpartanPlayer getPlayer() {
        return player;
    }

    public World getWorld() {
        return serverWorld;
    }

    public Chunk getChunk() {
        if (chunk == null) {
            chunk = serverWorld.getChunkAt(getChunkX(), getChunkZ());
        }
        return chunk;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setX(double x) {
        if (this.block != null) {
            if (this.chunk != null) {
                int currentX = getBlockX(), chunkX = (currentX >> 4);
                this.x = x;
                int newX = getBlockX();

                if (currentX != newX) {
                    this.block = null;

                    if (chunkX != (newX >> 4)) {
                        this.chunk = null;
                    }
                }
            } else {
                int currentX = getBlockX();
                this.x = x;

                if (currentX != getBlockX()) {
                    this.block = null;
                }
            }
        } else if (this.chunk != null) {
            int chunkX = getChunkX();
            this.x = x;

            if (chunkX != getChunkX()) {
                this.chunk = null;
            }
        } else {
            this.x = x;
        }
    }

    public void setY(double y) {
        if (this.block != null) {
            int currentY = getBlockY();
            this.y = y;

            if (currentY != getBlockY()) {
                this.block = null;
            }
        } else {
            this.y = y;
        }
    }

    public void setZ(double z) {
        if (this.block != null) {
            if (this.chunk != null) {
                int currentZ = getBlockZ(), chunkZ = (currentZ >> 4);
                this.z = z;
                int newZ = getBlockZ();

                if (currentZ != newZ) {
                    this.block = null;

                    if (chunkZ != (newZ >> 4)) {
                        this.chunk = null;
                    }
                }
            } else {
                int currentZ = getBlockZ();
                this.z = z;

                if (currentZ != getBlockZ()) {
                    this.block = null;
                }
            }
        } else if (this.chunk != null) {
            int chunkZ = getChunkZ();
            this.z = z;

            if (chunkZ != getChunkZ()) {
                this.chunk = null;
            }
        } else {
            this.z = z;
        }
    }

    public void setYaw(float yaw) {
        if (yaw > 360.0f) {
            this.yaw = 360.0f;
        } else {
            this.yaw = Math.max(yaw, 0.0f);
        }
        this.vector = null;
    }

    public void setPitch(float pitch) {
        if (pitch > 90.0f) {
            this.pitch = 90.0f;
        } else {
            this.pitch = Math.max(pitch, -90.0f);
        }
        this.vector = null;
    }

    public void setDirection(Vector vector) {
        this.vector = vector;
    }

    public int getBlockX() {
        return AlgebraUtils.integerFloor(x);
    }

    public int getBlockY() {
        return AlgebraUtils.integerFloor(y);
    }

    public int getBlockZ() {
        return AlgebraUtils.integerFloor(z);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public int getChunkX() {
        return getBlockX() >> 4;
    }

    public int getChunkZ() {
        return getBlockZ() >> 4;
    }

    public int getLocalX() {
        return getBlockX() & 15;
    }

    public int getLocalY() {
        return Math.max(0, Math.min(getBlockY(), 255));
    }

    public int getLocalZ() {
        return getBlockZ() & 15;
    }

    public Vector getDirection() {
        return vector == null ? this.vector = CombatUtils.getDirection(this.yaw, this.pitch) : vector;
    }

    public Vector toVector() {
        return new Vector(x, y, z);
    }

    public SpartanLocation subtract(double x, double y, double z) {
        return add(-x, -y, -z);
    }

    public SpartanLocation subtract(SpartanLocation loc) {
        return subtract(loc.getX(), loc.getY(), loc.getZ());
    }

    public SpartanLocation subtract(Location loc) {
        return subtract(loc.getX(), loc.getY(), loc.getZ());
    }

    public SpartanLocation subtract(Vector vec) {
        return subtract(vec.getX(), vec.getY(), vec.getZ());
    }

    public SpartanLocation add(Vector vec) {
        return add(vec.getX(), vec.getY(), vec.getZ());
    }

    public SpartanLocation add(SpartanLocation loc) {
        return add(loc.getX(), loc.getY(), loc.getZ());
    }

    public SpartanLocation add(double x, double y, double z) {
        Location bukkit = getLimitedBukkitLocation().add(x, y, z);

        if (Math.abs(x) >= 1.0 || Math.abs(y) >= 1.0 || Math.abs(z) >= 1.0) {
            int startX = getChunkX(),
                    startZ = getChunkZ();

            this.x = bukkit.getX();
            this.y = bukkit.getY();
            this.z = bukkit.getZ();
            this.block = null;

            if (startX != getChunkX() || startZ != getChunkZ()) {
                this.chunk = null;
            }
        } else {
            int startX = getBlockX(),
                    startY = getBlockY(),
                    startZ = getBlockZ();

            this.x = bukkit.getX();
            this.y = bukkit.getY();
            this.z = bukkit.getZ();

            if (getBlockX() != startX || getBlockY() != startY || getBlockZ() != startZ) {
                this.block = null;

                if ((startX >> 4) != getChunkX() || (startZ >> 4) != getChunkZ()) {
                    this.chunk = null;
                }
            }
        }
        return this;
    }

    private SpartanBlock setBlock() {
        Block block = serverWorld.getBlockAt(getBlockX(), getBlockY(), getBlockZ());

        if (block != null) {
            if (v_1_13) {
                BlockData blockData = block.getBlockData();
                boolean waterLogged = blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
                return loadBlock(blockData.getMaterial(),
                        (byte) (blockData instanceof Levelled ? ((Levelled) blockData).getLevel() : 0),
                        waterLogged || BlockUtils.isLiquid(block),
                        waterLogged);
            } else {
                return loadBlock(block.getType(), block.getData(), BlockUtils.isLiquid(block), false);
            }
        } else {
            loadEmptyBlock(false);
            return null;
        }
    }

    public SpartanBlock getBlock() {
        if (block == null) {
            int blockY = getBlockY();

            if (v_1_17 ?
                    blockY >= serverWorld.getMinHeight() && blockY <= serverWorld.getMaxHeight() :
                    blockY >= 0 && blockY <= MoveUtils.height) {
                UUID uuid = serverWorld.getUID();
                int blockX = getBlockX(),
                        blockZ = getBlockZ(),
                        hash = uuid.hashCode();

                if (SpartanBukkit.isSynchronised() || Chunks.isLoaded(uuid, serverWorld, blockX >> 4, blockZ >> 4)) {
                    hash = (SpartanBukkit.hashCodeMultiplier * hash) + blockX;
                    hash = (SpartanBukkit.hashCodeMultiplier * hash) + blockY;
                    hash = (SpartanBukkit.hashCodeMultiplier * hash) + blockZ;
                    BlockCache cache = memory.get(hash);

                    if (cache != null) {
                        this.block = cache.block;
                    } else {
                        SpartanBlock block = setBlock();

                        if (block != null) {
                            memory.put(hash, new BlockCache(block));
                        }
                    }
                } else {
                    loadEmptyBlock(false);
                }
            } else {
                loadEmptyBlock(true);
            }
        }
        return block;
    }

    public void retrieveDataFrom(SpartanLocation other) {
        if (this.hashCode() != other.hashCode()) {
            if (this.block == null) {
                SpartanBlock block = other.block;

                if (block != null
                        && this.getBlockX() == other.getBlockX()
                        && this.getBlockY() == other.getBlockY()
                        && this.getBlockZ() == other.getBlockZ()) {
                    this.block = block;

                    if (this.chunk == null) {
                        this.chunk = other.chunk;
                    }
                }
            } else if (this.chunk == null) {
                Chunk chunk = other.chunk;

                if (chunk != null
                        && this.getChunkX() == other.getChunkX()
                        && this.getChunkZ() == other.getChunkZ()) {
                    this.chunk = chunk;
                }
            }
        }
    }

    public double distance(SpartanLocation loc) {
        this.retrieveDataFrom(loc);
        return AlgebraUtils.getDistance(this.x, loc.getX(), this.y, loc.getY(), this.z, loc.getZ());
    }

    public double distance(Location loc) {
        return AlgebraUtils.getDistance(this.x, loc.getX(), this.y, loc.getY(), this.z, loc.getZ());
    }

    public Location getBukkitLocation() {
        return new Location(this.serverWorld, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    public Location getLimitedBukkitLocation() {
        return new Location(this.serverWorld, this.x, this.y, this.z);
    }

    // Custom

    // Direction

    public boolean changedDirection(SpartanLocation location) {
        return getPitch() != location.getPitch() || getYaw() != location.getYaw();
    }

    public boolean changedBlock(SpartanLocation location, boolean elevated) {
        return getBlockX() != location.getBlockX() || getBlockZ() != location.getBlockZ()
                || (elevated ? getBlockY() > location.getBlockY() : getBlockY() != location.getBlockY());
    }

    public BlockFace getDirectionFace() {
        float yaw = getYaw();

        if (yaw < 0.0f) {
            yaw += 360.0f;
        }
        yaw %= 360;

        switch ((int) ((yaw + 8) / 22.5)) {
            case 1:
                return BlockFace.WEST_NORTH_WEST;
            case 2:
                return BlockFace.NORTH_WEST;
            case 3:
                return BlockFace.NORTH_NORTH_WEST;
            case 4:
                return BlockFace.NORTH;
            case 5:
                return BlockFace.NORTH_NORTH_EAST;
            case 6:
                return BlockFace.NORTH_EAST;
            case 7:
                return BlockFace.EAST_NORTH_EAST;
            case 8:
                return BlockFace.EAST;
            case 9:
                return BlockFace.EAST_SOUTH_EAST;
            case 10:
                return BlockFace.SOUTH_EAST;
            case 11:
                return BlockFace.SOUTH_SOUTH_EAST;
            case 12:
                return BlockFace.SOUTH;
            case 13:
                return BlockFace.SOUTH_SOUTH_WEST;
            case 14:
                return BlockFace.SOUTH_WEST;
            case 15:
                return BlockFace.WEST_SOUTH_WEST;
            default:
                return BlockFace.WEST;
        }
    }

    // Surrounding

    public List<SpartanLocation> getSurroundingLocations(double x, double y, double z, boolean all) {
        List<SpartanLocation> array = new ArrayList<>(9);
        double dx = getX() - getBlockX(),
                dz = getZ() - getBlockZ();

        if (y == 0.0) {
            array.add(this);
        } else {
            array.add(this.clone().add(0, y, 0));
        }

        if (!all) {
            if (dx >= 0.0 && dx < 0.3 && dz >= 0.0 && dz < 0.3) {
                // 300 300
                array.add(this.clone().add(0, y, -z));
                array.add(this.clone().add(-x, y, 0));
                array.add(this.clone().add(-x, y, -z));
            } else if (dx > 0.7 && dx < 1.0 && dz >= 0.0 && dz < 0.3) {
                // 700 300
                array.add(this.clone().add(x, y, 0));
                array.add(this.clone().add(0, y, -z));
                array.add(this.clone().add(x, y, -z));
            } else if (dx > 0.7 && dx < 1.0 && dz > 0.7 && dz < 1.0) {
                // 700 700
                array.add(this.clone().add(x, y, 0));
                array.add(this.clone().add(0, y, z));
                array.add(this.clone().add(x, y, z));
            } else if (dx >= 0.0 && dx < 0.3 && dz > 0.7 && dz < 1.0) {
                // 300 700
                array.add(this.clone().add(-x, y, 0));
                array.add(this.clone().add(0, y, z));
                array.add(this.clone().add(-x, y, z));
            } else if (dx >= 0.0 && dx < 0.3 && dz > 0.3 && dz < 0.7) {
                // 300 300-700
                array.add(this.clone().add(x, y, 0));
            } else if (dx > 0.7 && dx < 1.0 && dz > 0.3 && dz < 0.7) {
                // 700 300-700
                array.add(this.clone().add(-x, y, 0));
            } else if (dx > 0.3 && dx < 0.7 && dz >= 0.0 && dz < 0.3) {
                // 300-700 300
                array.add(this.clone().add(0, y, -z));
            } else if (dx > 0.3 && dx < 0.7 && dz > 0.7 && dz < 1.0) {
                // 300-700 700
                array.add(this.clone().add(0, y, z));
            }
        } else {
            array.add(this.clone().add(x, y, 0));
            array.add(this.clone().add(-x, y, 0));
            array.add(this.clone().add(0, y, z));
            array.add(this.clone().add(0, y, -z));
            array.add(this.clone().add(x, y, z));
            array.add(this.clone().add(-x, y, -z));
            array.add(this.clone().add(x, y, -z));
            array.add(this.clone().add(-x, y, z));
        }
        return array;
    }

    public List<SpartanLocation> getSurroundingLocations(double x, double y, double z) {
        if (x < 1.0 || z < 1.0) {
            return getSurroundingLocations(x, y, z,
                    player == null
                            || player.isBedrockPlayer()
                            || player.getVehicle() != null
                            || player.getActivePotionEffects().length > 0
                            || BlockUtils.isSolid(player.getItemInHand().getType())
                            || Check.hasViolations(player)
                            || Check.getLastViolation(player, true) <= Check.violationCycleSeconds
                            || player.getProfile().isSuspectedOrHacker()
            );
        }
        return getSurroundingLocations(x, y, z, false);
    }

    public int getSurroundingSolidBlockCount(double x, double z, int cap) {
        int count = 0;

        for (SpartanLocation location : new SpartanLocation[]{
                this.clone().add(x, 0, 0),
                this.clone().add(-x, 0, 0),
                this.clone().add(0, 0, z),
                this.clone().add(0, 0, -z),
                /*loc.clone().add(x, 0, z),
                loc.clone().add(-x, 0, -z),
                loc.clone().add(x, 0, -z),
                loc.clone().add(-x, 0, z)*/
        }) {
            if (BlockUtils.isSolid(location.getBlock().getType())) {
                count++;

                if (count == cap) {
                    break;
                }
            }
        }
        return count;
    }

    // Identifiers

    public boolean isType(Material m) {
        return getBlock().getType() == m;
    }

    public boolean isBlock(Material[] m, double i) {
        for (SpartanLocation loc : getSurroundingLocations(i, 0, i, true)) {
            for (Material material : m) {
                if (loc.isType(material)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isBlock(Material m, double i) {
        return isBlock(new Material[]{m}, i);
    }

    public boolean areBlocks(Material[] m, double i) {
        for (SpartanLocation loc : getSurroundingLocations(i, 0, i)) {
            for (Material material : m) {
                if (!loc.isType(material)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean areBlocks(Material m, double i) {
        return areBlocks(new Material[]{m}, i);
    }

    public boolean isBlockInBetween(SpartanLocation from, int limit, boolean protection) {
        if (AlgebraUtils.getHorizontalDistance(this, from) <= 1.5) {
            int newY = getBlockY();
            int oldY = from.getBlockY();
            double diff = Math.abs(getY() - from.getY());

            if (diff > 1.0 && diff <= limit) {
                for (int i = Math.min(oldY, newY) + 1; i < Math.max(oldY, newY); i++) {
                    if (BlockUtils.isSolid(clone().add(0, -i, 0).getBlock().getType())
                            || BlockUtils.isSolid(from.clone().add(0, i, 0).getBlock().getType())) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return protection;
    }

    public boolean isSameChunk(SpartanLocation location) {
        return this.getChunkX() == location.getChunkX()
                && this.getChunkZ() == location.getChunkZ();
    }

    public boolean isConnectedChunk(SpartanLocation location) {
        return Math.abs(this.getChunkX() - location.getChunkX()) == 1
                && Math.abs(this.getChunkZ() - location.getChunkZ()) == 1;
    }
}
