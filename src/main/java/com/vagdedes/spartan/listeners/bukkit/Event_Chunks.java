package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanBlockData;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Event_Chunks implements Listener {

    private static final Map<Long, Boolean> loaded = new ConcurrentHashMap<>();
    private static final Map<World, Map<Long, ChunkData>> map = new ConcurrentHashMap<>();
    private static final int chunkSize = (int) PlayerUtils.chunk;
    private static final SpartanBlockData empty = new SpartanBlockData(Material.AIR, false);

    // Separator

    private static final class ChunkData {

        private long creation;
        private final int x, z;
        private final SpartanBlockData[][][] blocks;

        private ChunkData(World world, ChunkSnapshot snapshot) {
            int minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                    ? world.getMinHeight()
                    : 0,
                    maxHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                            ? world.getMaxHeight()
                            : PlayerUtils.height;
            this.creation = System.currentTimeMillis();
            this.x = snapshot.getX();
            this.z = snapshot.getZ();
            this.blocks = new SpartanBlockData[chunkSize][maxHeight + Math.abs(minHeight)][chunkSize];
            SpartanBukkit.chunksThread.execute(() -> this.set(world, snapshot));
        }

        private void refresh(World world) {
            if (System.currentTimeMillis() - this.creation > 1_000L) {
                SpartanBukkit.chunksThread.executeIfFree(() -> {
                    this.creation = System.currentTimeMillis();
                    this.set(world, world.getChunkAt(this.x, this.z).getChunkSnapshot());
                });
            }
        }

        private void set(World world, ChunkSnapshot snapshot) {
            int minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                    ? world.getMinHeight()
                    : 0,
                    maxHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                            ? world.getMaxHeight()
                            : PlayerUtils.height,
                    subtract;

            if (minHeight < 0) {
                subtract = Math.abs(minHeight);
                maxHeight += subtract;
                minHeight = 0;
            } else {
                subtract = 0;
            }
            for (int x = 0; x < chunkSize; x++) {
                for (int z = 0; z < chunkSize; z++) {
                    for (int y = minHeight; y < maxHeight; y++) {
                        BlockData blockData = snapshot.getBlockData(x, y - subtract, z);
                        this.blocks[x][y][z] = new SpartanBlockData(
                                blockData.getMaterial(),
                                BlockUtils.isWaterLogged(blockData)
                        );
                    }
                }
            }
        }

    }

    // Separator

    static {
        if (SpartanBukkit.packetsForcedState) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (SpartanBukkit.packetsEnabled()) {
                    List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                    if (!players.isEmpty()) {
                        for (SpartanPlayer player : players) {
                            SpartanLocation location = player.movement.getRawLocation();
                            int chunkX = location.getChunkX(),
                                    chunkZ = location.getChunkZ();

                            if (isLoaded(location.world, chunkX, chunkZ)) {
                                cache(location.getBukkitLocation().getChunk(), true);
                            }
                        }
                    }
                }
            }, 1L, 1L);
        }
    }

    // Separator

    private static long hashChunk(int x, int z) {
        return (31L * x) + z;
    }

    private static long totalHash(World world, int x, int z) {
        return (hashChunk(x, z) * 31L) + world.getName().hashCode();
    }

    // Separator

    public static void clear() {
        map.clear();
    }

    public static boolean isLoaded(World world, int x, int z) {
        if (loaded.containsKey(totalHash(world, x, z))) {
            return true;
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.getX() == x && chunk.getZ() == z) {
                loaded.put(totalHash(chunk.getWorld(), chunk.getX(), chunk.getZ()), true);
                return true;
            }
        }
        return false;
    }

    public static SpartanBlockData get(World world, int x, int y, int z) {
        int minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                ? world.getMinHeight()
                : 0;

        if (y < minHeight
                || y >= (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                ? world.getMaxHeight()
                : PlayerUtils.height)) {
            return empty;
        }
        Map<Long, ChunkData> subMap = map.get(world);

        if (subMap == null) {
            return empty;
        }
        ChunkData blockDataList = subMap.get(hashChunk(SpartanLocation.getChunkPos(x), SpartanLocation.getChunkPos(z)));

        if (blockDataList == null
                || y >= blockDataList.blocks[0].length) {
            return empty;
        }
        if (minHeight < 0) {
            y += Math.abs(minHeight);
        }
        return blockDataList.blocks[Math.abs(x) % chunkSize][y][Math.abs(z) % chunkSize];
    }

    // Separator

    static void updateBlock(Block block, BlockData blockData) {
        World world = block.getWorld();
        int x = block.getX(),
                z = block.getZ();
        Map<Long, ChunkData> subMap = map.get(world);

        if (subMap == null) {
            cache(block.getChunk(), true);
        } else {
            ChunkData chunkData = subMap.get(hashChunk(SpartanLocation.getChunkPos(x), SpartanLocation.getChunkPos(z)));

            if (chunkData == null) {
                cache(block.getChunk(), true);
            } else {
                int y = block.getY(),
                        minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? world.getMinHeight() : 0;

                if (minHeight < 0) {
                    y += Math.abs(minHeight);
                }
                if (blockData == null) {
                    chunkData.blocks[Math.abs(x) % chunkSize][y][Math.abs(z) % chunkSize] = new SpartanBlockData(
                            Material.AIR,
                            false
                    );
                } else {
                    chunkData.blocks[Math.abs(x) % chunkSize][y][Math.abs(z) % chunkSize] = new SpartanBlockData(
                            blockData.getMaterial(),
                            BlockUtils.isWaterLogged(blockData)
                    );
                }
            }
        }
    }

    // Separator

    private static boolean hasPlayers(World world, int x, int z) {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer player : players) {
                SpartanLocation location = player.movement.getRawLocation();

                if (location.world.equals(world)
                        && location.getChunkX() == x
                        && location.getChunkZ() == z) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void cache(Chunk chunk, boolean force) {
        World world = chunk.getWorld();

        if (!force && !hasPlayers(world, chunk.getX(), chunk.getZ())) {
            return;
        }
        long hash = hashChunk(chunk.getX(), chunk.getZ());
        Map<Long, ChunkData> subMap = map.computeIfAbsent(
                world,
                k -> new ConcurrentHashMap<>()
        );
        ChunkData chunkData = subMap.get(hash);

        if (chunkData == null) {
            subMap.put(hash, new ChunkData(world, chunk.getChunkSnapshot()));
        } else {
            chunkData.refresh(world);
        }
    }

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Load(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.put(totalHash(chunk.getWorld(), chunk.getX(), chunk.getZ()), true);

        if (SpartanBukkit.packetsEnabled()) {
            cache(chunk, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Unload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.remove(totalHash(chunk.getWorld(), chunk.getX(), chunk.getZ()));

        if (SpartanBukkit.packetsEnabled()) {
            World world = chunk.getWorld();
            Map<Long, ChunkData> subMap = map.get(world);

            if (subMap != null
                    && subMap.remove(hashChunk(chunk.getX(), chunk.getZ())) != null
                    && subMap.isEmpty()) {
                map.remove(world);
            }
        }
    }

}
